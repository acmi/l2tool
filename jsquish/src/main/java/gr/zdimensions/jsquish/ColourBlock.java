/* -----------------------------------------------------------------------------

	Copyright (c) 2006 Simon Brown                          si@sjbrown.co.uk

	Permission is hereby granted, free of charge, to any person obtaining
	a copy of this software and associated documentation files (the
	"Software"), to	deal in the Software without restriction, including
	without limitation the rights to use, copy, modify, merge, publish,
	distribute, sublicense, and/or sell copies of the Software, and to
	permit persons to whom the Software is furnished to do so, subject to
	the following conditions:

	The above copyright notice and this permission notice shall be included
	in all copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
	OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
	MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
	IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
	CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
	TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
	SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

   -------------------------------------------------------------------------- */

package gr.zdimensions.jsquish;

import static java.lang.Math.*;
import java.util.Arrays;

import static gr.zdimensions.jsquish.CompressorColourFit.*;

final class ColourBlock {

	private static final int[] remapped = new int[16];

	private static final int[] indices = new int[16];

	private static final int[] codes = new int[16];

	private ColourBlock() {}

	static int gammaColour(final float colour, final float scale) {
		//return round(scale * (float)Math.pow(colour, 1.0 / 2.2));
		return round(scale * colour);
	}

	private static int floatTo565(final Vec colour) {
		// get the components in the correct range
		final int r = round(GRID_X * colour.x());
		final int g = round(GRID_Y * colour.y());
		final int b = round(GRID_Z * colour.z());

		// pack into a single value
		return (r << 11) | (g << 5) | b;
	}

	private static void writeColourBlock(final int a, final int b, final int[] indices, final byte[] block, final int offset) {
		// write the endpoints
		block[offset + 0] = (byte)(a & 0xff);
		block[offset + 1] = (byte)(a >> 8);
		block[offset + 2] = (byte)(b & 0xff);
		block[offset + 3] = (byte)(b >> 8);

		// write the indices
		for ( int i = 0; i < 4; ++i ) {
			final int index = 4 * i;
			block[offset + 4 + i] = (byte)(indices[index + 0] | (indices[index + 1] << 2) | (indices[index + 2] << 4) | (indices[index + 3] << 6));
		}
	}

	static void writeColourBlock3(final Vec start, final Vec end, final int[] indices, final byte[] block, final int offset) {
		// get the packed values
		int a = floatTo565(start);
		int b = floatTo565(end);

		// remap the indices
		if ( a <= b ) {
			// use the indices directly
			System.arraycopy(indices, 0, remapped, 0, 16);
		} else {
			// swap a and b
			final int tmp = a;
			a = b;
			b = tmp;
			for ( int i = 0; i < 16; ++i ) {
				if ( indices[i] == 0 )
					remapped[i] = 1;
				else if ( indices[i] == 1 )
					remapped[i] = 0;
				else
					remapped[i] = indices[i];
			}
		}

		// write the block
		writeColourBlock(a, b, remapped, block, offset);
	}

	static void writeColourBlock4(final Vec start, final Vec end, final int[] indices, final byte[] block, final int offset) {
		// get the packed values
		int a = floatTo565(start);
		int b = floatTo565(end);

		// remap the indices

		if ( a < b ) {
			// swap a and b
			final int tmp = a;
			a = b;
			b = tmp;
			for ( int i = 0; i < 16; ++i )
				remapped[i] = (indices[i] ^ 0x1) & 0x3;
		} else if ( a == b ) {
			// use index 0
			Arrays.fill(remapped, 0);
		} else {
			// use the indices directly
			System.arraycopy(indices, 0, remapped, 0, 16);
		}

		// write the block
		writeColourBlock(a, b, remapped, block, offset);
	}

	static void decompressColour(final byte[] rgba, final byte[] block, final int offset, final boolean isDXT1) {
		// unpack the endpoints
		final int[] codes = ColourBlock.codes;

		final int a = unpack565(block, offset, codes, 0);
		final int b = unpack565(block, offset + 2, codes, 4);

		// generate the midpoints
		for ( int i = 0; i < 3; ++i ) {
			final int c = codes[i];
			final int d = codes[4 + i];

			if ( isDXT1 && a <= b ) {
				codes[8 + i] = (c + d) / 2;
				codes[12 + i] = 0;
			} else {
				codes[8 + i] = (2 * c + d) / 3;
				codes[12 + i] = (c + 2 * d) / 3;
			}
		}

		// fill in alpha for the intermediate values
		codes[8 + 3] = 255;
		codes[12 + 3] = (isDXT1 && a <= b) ? 0 : 255;

		// unpack the indices
		final int[] indices = ColourBlock.indices;

		for ( int i = 0; i < 4; ++i ) {
			final int index = 4 * i;
			final int packed = (block[offset + 4 + i] & 0xFF);

			indices[index + 0] = packed & 0x3;
			indices[index + 1] = (packed >> 2) & 0x3;
			indices[index + 2] = (packed >> 4) & 0x3;
			indices[index + 3] = (packed >> 6) & 0x3;
		}

		// store out the colours
		for ( int i = 0; i < 16; ++i ) {
			final int index = 4 * indices[i];
			for ( int j = 0; j < 4; ++j )
				rgba[4 * i + j] = (byte)codes[index + j];
		}
	}

	private static int unpack565(final byte[] packed, final int pOffset, final int[] colour, final int cOffset) {
		// build the packed value
		int value = (packed[pOffset + 0] & 0xff) | ((packed[pOffset + 1] & 0xff) << 8);

		// get the components in the stored range
		int red = (value >> 11) & 0x1f;
		int green = (value >> 5) & 0x3f;
		int blue = value & 0x1f;

		// scale up to 8 bits
		colour[cOffset + 0] = (red << 3) | (red >> 2);
		colour[cOffset + 1] = (green << 2) | (green >> 4);
		colour[cOffset + 2] = (blue << 3) | (blue >> 2);
		colour[cOffset + 3] = 255;

		// return the value
		return value;
	}

}