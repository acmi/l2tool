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

import static java.lang.Math.round;

import static gr.zdimensions.jsquish.ColourBlock.*;
import static gr.zdimensions.jsquish.SingleColourLookup3.*;
import static gr.zdimensions.jsquish.SingleColourLookup4.*;
import gr.zdimensions.jsquish.Squish.CompressionType;

final class CompressorSingleColour extends CompressorColourFit {

	private static final int[] indices = new int[16];

	private static final int[][][][] lookups = new int[3][][][];

	private static int[][] sources = new int[3][];

	private static final Vec start = new Vec();
	private static final Vec end = new Vec();

	private static int[] index = new int[1];

	private static int bestError;

	private int[] colour = new int[3];

	CompressorSingleColour(final ColourSet colours, final CompressionType type) {
		super(colours, type);

		// grab the single colour
		final Vec colour = colours.getPoints()[0];
		this.colour[0] = round(255.0f * colour.x());
		this.colour[1] = round(255.0f * colour.y());
		this.colour[2] = round(255.0f * colour.z());

		// initialise the best error
		bestError = Integer.MAX_VALUE;
	}

	void compress3(final byte[] block, final int offset) {
		// build the table of lookups
		lookups[0] = LOOKUP_5_3;
		lookups[1] = LOOKUP_6_3;
		lookups[2] = LOOKUP_5_3;

		// find the best end-points and index
		final int error = computeEndPoints(3, lookups);

		// build the block if we win
		if ( error < bestError ) {
			// remap the indices
			colours.remapIndices(index, indices);

			// save the block
			writeColourBlock3(start, end, indices, block, offset);

			// save the error
			bestError = error;
		}
	}

	void compress4(final byte[] block, final int offset) {
		// build the table of lookups
		lookups[0] = LOOKUP_5_4;
		lookups[1] = LOOKUP_6_4;
		lookups[2] = LOOKUP_5_4;

		// find the best end-points and index
		final int error = computeEndPoints(4, lookups);

		// build the block if we win
		if ( error < bestError ) {
			// remap the indices
			colours.remapIndices(index, indices);

			// save the block
			writeColourBlock4(start, end, indices, block, offset);

			// save the error
			bestError = error;
		}
	}

	private int computeEndPoints(final int count, final int[][][][] lookups) {
		final int[][] sources = CompressorSingleColour.sources;

		int bestError = CompressorSingleColour.bestError;

		// check each index combination
		for ( int index = 0; index < count; ++index ) {
			// check the error for this codebook index
			int error = 0;
			for ( int channel = 0; channel < 3; ++channel ) {
				// grab the lookup table and index for this channel
				final int[][][] lookup = lookups[channel];
				final int target = colour[channel];

				// store a pointer to the source for this channel
				sources[channel] = lookup[target][index];

				// accumulate the error
				final int diff = sources[channel][2];
				error += diff * diff;
			}

			// keep it if the error is lower
			if ( error < bestError ) {
				start.set(sources[0][0] * GRID_X_RCP,
						  sources[1][0] * GRID_Y_RCP,
						  sources[2][0] * GRID_Z_RCP);

				end.set(sources[0][1] * GRID_X_RCP,
						sources[1][1] * GRID_Y_RCP,
						sources[2][1] * GRID_Z_RCP);

				CompressorSingleColour.index[0] = index;
				bestError = error;
			}
		}

		return bestError;
	}

}