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

final class CompressorAlpha {

	private static final int[] swapped = new int[16];

	private static final int[] codes5 = new int[8];
	private static final int[] codes7 = new int[8];

	private static final int[] indices5 = new int[16];
	private static final int[] indices7 = new int[16];

	private static final int[] codes = new int[8];
	private static final int[] indices = new int[16];

	private CompressorAlpha() {}

	static void compressAlphaDxt3(final byte[] rgba, final int mask, final byte[] block, final int offset) {
		// quantise and pack the alpha values pairwise
		for ( int i = 0; i < 8; ++i ) {
			// quantise down to 4 bits
			final float alpha1 = (rgba[8 * i + 3] & 0xFF) * (15.0f / 255.0f);
			final float alpha2 = (rgba[8 * i + 7] & 0xFF) * (15.0f / 255.0f);
			int quant1 = Math.round(alpha1);
			int quant2 = Math.round(alpha2);

			// set alpha to zero where masked
			final int bit1 = 1 << (2 * i);
			final int bit2 = 1 << (2 * i + 1);
			if ( (mask & bit1) == 0 )
				quant1 = 0;
			if ( (mask & bit2) == 0 )
				quant2 = 0;

			// pack into the byte
			block[offset + i] = (byte)(quant1 | (quant2 << 4));
		}
	}

	static void decompressAlphaDxt3(final byte[] rgba, final byte[] block, final int offset) {
		// unpack the alpha values pairwise
		for ( int i = 0; i < 8; ++i ) {
			// quantise down to 4 bits
			final int quant = (block[offset + i] & 0xFF);

			// unpack the values
			int lo = quant & 0x0f;
			int hi = quant & 0xf0;

			// convert back up to bytes
			rgba[8 * i + 3] = (byte)(lo | (lo << 4));
			rgba[8 * i + 7] = (byte)(hi | (hi >> 4));
		}
	}

	private static int fitCodes(final byte[] rgba, final int mask, final int[] codes, final int[] indices) {
		// fit each alpha value to the codebook
		int err = 0;
		for ( int i = 0; i < 16; ++i ) {
			// check this pixel is valid
			final int bit = 1 << i;
			if ( (mask & bit) == 0 ) {
				// use the first code
				indices[i] = 0;
				continue;
			}

			// find the least error and corresponding index
			final int value = (rgba[4 * i + 3] & 0xFF);
			int least = Integer.MAX_VALUE;
			int index = 0;
			for ( int j = 0; j < 8; ++j ) {
				// get the squared error from this code
				int dist = value - codes[j];
				dist *= dist;

				// compare with the best so far
				if ( dist < least ) {
					least = dist;
					index = j;
				}
			}

			// save this index and accumulate the error
			indices[i] = index;
			err += least;
		}

		// return the total error
		return err;
	}

	private static void writeAlphaBlock(final int alpha0, final int alpha1, final int[] indices, final byte[] block, final int offset) {
		// write the first two bytes
		block[offset + 0] = (byte)alpha0;
		block[offset + 1] = (byte)alpha1;

		// pack the indices with 3 bits each
		int src = 0;
		int dest = 2;
		for ( int i = 0; i < 2; ++i ) {
			// pack 8 3-bit values
			int value = 0;
			for ( int j = 0; j < 8; ++j ) {
				final int index = indices[src++];
				value |= (index << 3 * j);
			}

			// store in 3 bytes
			for ( int j = 0; j < 3; ++j )
				block[offset + dest++] = (byte)((value >> 8 * j) & 0xff);
		}
	}

	private static void writeAlphaBlock5(final int alpha0, final int alpha1, final int[] indices, final byte[] block, final int offset) {
		// check the relative values of the endpoints
		final int[] swapped = CompressorAlpha.swapped;

		if ( alpha0 > alpha1 ) {
			// swap the indices
			for ( int i = 0; i < 16; ++i ) {
				int index = indices[i];
				if ( index == 0 )
					swapped[i] = 1;
				else if ( index == 1 )
					swapped[i] = 0;
				else if ( index <= 5 )
					swapped[i] = 7 - index;
				else
					swapped[i] = index;
			}

			// write the block
			writeAlphaBlock(alpha1, alpha0, swapped, block, offset);
		} else {
			// write the block
			writeAlphaBlock(alpha0, alpha1, indices, block, offset);
		}
	}

	private static void writeAlphaBlock7(final int alpha0, final int alpha1, final int[] indices, final byte[] block, final int offset) {
		// check the relative values of the endpoints
		final int[] swapped = CompressorAlpha.swapped;

		if ( alpha0 < alpha1 ) {
			// swap the indices
			for ( int i = 0; i < 16; ++i ) {
				int index = indices[i];
				if ( index == 0 )
					swapped[i] = 1;
				else if ( index == 1 )
					swapped[i] = 0;
				else
					swapped[i] = 9 - index;
			}

			// write the block
			writeAlphaBlock(alpha1, alpha0, swapped, block, offset);
		} else {
			// write the block
			writeAlphaBlock(alpha0, alpha1, indices, block, offset);
		}
	}

	static void compressAlphaDxt5(final byte[] rgba, final int mask, final byte[] block, final int offset) {
		// get the range for 5-alpha and 7-alpha interpolation
		int min5 = 255;
		int max5 = 0;
		int min7 = 255;
		int max7 = 0;
		for ( int i = 0; i < 16; ++i ) {
			// check this pixel is valid
			final int bit = 1 << i;
			if ( (mask & bit) == 0 )
				continue;

			// incorporate into the min/max
			final int value = (rgba[4 * i + 3] & 0xFF);
			if ( value < min7 )
				min7 = value;
			if ( value > max7 )
				max7 = value;
			if ( value != 0 && value < min5 )
				min5 = value;
			if ( value != 255 && value > max5 )
				max5 = value;
		}

		// handle the case that no valid range was found
		if ( min5 > max5 )
			min5 = max5;
		if ( min7 > max7 )
			min7 = max7;

		// fix the range to be the minimum in each case
		if ( max5 - min5 < 5 )
			max5 = min(min5 + 5, 255);
		if ( max5 - min5 < 5 )
			min5 = max(0, max5 - 5);

		if ( max7 - min7 < 7 )
			max7 = min(min7 + 7, 255);
		if ( max7 - min7 < 7 )
			min7 = max(0, max7 - 7);

		// set up the 5-alpha code book
		final int[] codes5 = CompressorAlpha.codes5;

		codes5[0] = min5;
		codes5[1] = max5;
		for ( int i = 1; i < 5; ++i )
			codes5[1 + i] = ((5 - i) * min5 + i * max5) / 5;
		codes5[6] = 0;
		codes5[7] = 255;

		// set up the 7-alpha code book
		final int[] codes7 = CompressorAlpha.codes7;

		codes7[0] = min7;
		codes7[1] = max7;
		for ( int i = 1; i < 7; ++i )
			codes7[1 + i] = ((7 - i) * min7 + i * max7) / 7;

		// fit the data to both code books
		int err5 = fitCodes(rgba, mask, codes5, indices5);
		int err7 = fitCodes(rgba, mask, codes7, indices7);

		// save the block with least error
		if ( err5 <= err7 )
			writeAlphaBlock5(min5, max5, indices5, block, offset);
		else
			writeAlphaBlock7(min7, max7, indices7, block, offset);
	}

	static void decompressAlphaDxt5(final byte[] rgba, final byte[] block, final int offset) {
		// get the two alpha values
		final int alpha0 = (block[offset + 0] & 0xFF);
		final int alpha1 = (block[offset + 1] & 0xFF);

		// compare the values to build the codebook
		final int[] codes = CompressorAlpha.codes;

		codes[0] = alpha0;
		codes[1] = alpha1;
		if ( alpha0 <= alpha1 ) {
			// use 5-alpha codebook
			for ( int i = 1; i < 5; ++i )
				codes[1 + i] = ((5 - i) * alpha0 + i * alpha1) / 5;
			codes[6] = 0;
			codes[7] = 255;
		} else {
			// use 7-alpha codebook
			for ( int i = 1; i < 7; ++i )
				codes[1 + i] = ((7 - i) * alpha0 + i * alpha1) / 7;
		}

		// decode the indices
		final int[] indices = CompressorAlpha.indices;

		int src = 2;
		int dest = 0;
		for ( int i = 0; i < 2; ++i ) {
			// grab 3 bytes
			int value = 0;
			for ( int j = 0; j < 3; ++j ) {
				int b = (block[offset + src++] & 0xFF);
				value |= (b << 8 * j);
			}

			// unpack 8 3-bit values from it
			for ( int j = 0; j < 8; ++j ) {
				int index = (value >> 3 * j) & 0x7;
				indices[dest++] = index;
			}
		}

		// write out the indexed codebook values
		for ( int i = 0; i < 16; ++i )
			rgba[4 * i + 3] = (byte)codes[indices[i]];
	}

}