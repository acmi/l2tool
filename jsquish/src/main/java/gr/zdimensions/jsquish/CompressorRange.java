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

import gr.zdimensions.jsquish.Squish.CompressionMetric;
import gr.zdimensions.jsquish.Squish.CompressionType;

final class CompressorRange extends CompressorColourFit {

	private static final int[] closest = new int[16];

	private static final int[] indices = new int[16];

	private static final Vec[] codes = new Vec[4];

	static {
		for ( int i = 0; i < codes.length; i++ )
			codes[i] = new Vec();
	}

	private final CompressionMetric metric;
	private final Vec start = new Vec();
	private final Vec end = new Vec();

	private float bestError;

	CompressorRange(final ColourSet colours, final CompressionType type, final CompressionMetric metric) {
		super(colours, type);

		// initialise the metric
		this.metric = metric;

		// initialise the best error
		bestError = Float.MAX_VALUE;

		// cache some values
		final int count = this.colours.getCount();
		final Vec[] points = this.colours.getPoints();

		// get the covariance matrix
		final Matrix covariance = Matrix.computeWeightedCovariance(colours, CompressorColourFit.covariance);

		// compute the principle component
		final Vec principle = Matrix.computePrincipleComponent(covariance);

		// get the min and max range as the codebook endpoints
		if ( count > 0 ) {
			float aX, aY, aZ;
			float bX, bY, bZ;

			float min, max;

			// compute the range
			aX = bX = points[0].x();
			aY = bY = points[0].y();
			aZ = bZ = points[0].z();
			min = max = points[0].dot(principle);
			for ( int i = 1; i < count; ++i ) {
				final Vec p = points[i];
				final float val = p.dot(principle);

				if ( val < min ) {
					aX = p.x();
					aY = p.y();
					aZ = p.z();

					min = val;
				} else if ( val > max ) {
					bX = p.x();
					bY = p.y();
					bZ = p.z();

					max = val;
				}
			}

			// clamp the output to [0, 1] and to the grid
			aX = clamp(aX, GRID_X, GRID_X_RCP);
			aY = clamp(aY, GRID_Y, GRID_Y_RCP);
			aZ = clamp(aZ, GRID_Z, GRID_Z_RCP);

			start.set(aX, aY, aZ);

			bX = clamp(bX, GRID_X, GRID_X_RCP);
			bY = clamp(bY, GRID_Y, GRID_Y_RCP);
			bZ = clamp(bZ, GRID_Z, GRID_Z_RCP);

			end.set(bX, bY, bZ);
		}
	}

	void compress3(final byte[] block, final int offset) {
		// cache some values
		final int count = colours.getCount();
		final Vec[] points = colours.getPoints();

		final Vec v = new Vec();

		// create a codebook
		final Vec[] codes = CompressorRange.codes;
		codes[0].set(start);
		codes[1].set(end);
		codes[2].set(start).add(end).mul(0.5f);

		// match each point to the closest code
		final int[] closest = CompressorRange.closest;
		float error = 0.0f;
		for ( int i = 0; i < count; ++i ) {
			final Vec p = points[i];

			// find the closest code
			float dist = Float.MAX_VALUE;
			int index = 0;
			for ( int j = 0; j < 3; ++j ) {
				final Vec c = codes[j];
				v.set(
						(p.x() - c.x()) * metric.r,
						(p.y() - c.y()) * metric.g,
						(p.z() - c.z()) * metric.b
				);
				final float d = v.lengthSQ();
				if ( d < dist ) {
					dist = d;
					index = j;
				}
			}

			// save the index
			closest[i] = index;

			// accumulate the error
			error += dist;
		}

		// save this scheme if it wins
		if ( error < bestError ) {
			// remap the indices
			colours.remapIndices(closest, indices);

			// save the block
			ColourBlock.writeColourBlock3(start, end, indices, block, offset);

			// save the error
			bestError = error;
		}
	}

	void compress4(final byte[] block, final int offset) {
		// cache some values
		final int count = colours.getCount();
		final Vec[] points = colours.getPoints();

		final Vec v = new Vec();

		// create a codebook
		final Vec[] codes = CompressorRange.codes;
		codes[0].set(start);
		codes[1].set(end);
		codes[2].set(2.0f / 3.0f).mul(start).add(v.set(1.0f / 3.0f).mul(end));
		codes[3].set(1.0f / 3.0f).mul(start).add(v.set(2.0f / 3.0f).mul(end));

		// match each point to the closest code
		final int[] closest = CompressorRange.closest;
		float error = 0.0f;
		for ( int i = 0; i < count; ++i ) {
			final Vec p = points[i];

			// find the closest code
			float dist = Float.MAX_VALUE;
			int index = 0;
			for ( int j = 0; j < 4; ++j ) {
				final Vec c = codes[j];
				v.set(
						(p.x() - c.x()) * metric.r,
						(p.y() - c.y()) * metric.g,
						(p.z() - c.z()) * metric.b
				);
				final float d = v.lengthSQ();
				if ( d < dist ) {
					dist = d;
					index = j;
				}
			}

			// save the index
			closest[i] = index;

			// accumulate the error
			error += dist;
		}

		// save this scheme if it wins
		if ( error < bestError ) {
			// remap the indices
			colours.remapIndices(closest, indices);

			// save the block
			ColourBlock.writeColourBlock4(start, end, indices, block, offset);

			// save the error
			bestError = error;
		}
	}

}