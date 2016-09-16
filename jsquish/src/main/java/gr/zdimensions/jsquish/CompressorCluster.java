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

import gr.zdimensions.jsquish.Squish.CompressionType;
import gr.zdimensions.jsquish.Squish.CompressionMetric;

final class CompressorCluster extends CompressorColourFit {

	private static final int MAX_ITERATIONS = 8;

	private static final float TWO_THIRDS = 2.0f / 3.0f;
	private static final float ONE_THIRD = 1.0f / 3.0f;
	private static final float HALF = 0.5f;
	private static final float ZERO = 0.0f;

	private static Vec principle;

	private static final float[] dps = new float[16];

	private static final float[] weighted = new float[16 * 3];
	private static final float[] weights = new float[16];

	private static CompressionMetric metric;

	private static final int[] indices = new int[16];
	private static final int[] bestIndices = new int[16];

	private static final float[] alpha = new float[16];
	private static final float[] beta = new float[16];

	private static final int[] unordered = new int[16];

	private static final Vec xxSum = new Vec();

	private static float bestError;

	private static final int[] orders = new int[16 * MAX_ITERATIONS];

	CompressorCluster(final ColourSet colours, final CompressionType type, final CompressionMetric metric) {
		super(colours, type);

		// initialise the best error
		bestError = Float.MAX_VALUE;

		// initialise the metric
		CompressorCluster.metric = metric;

		// get the covariance matrix
		final Matrix covariance = Matrix.computeWeightedCovariance(colours, CompressorColourFit.covariance);

		// compute the principle component
		principle = Matrix.computePrincipleComponent(covariance);
	}

	void compress3(final byte[] block, final int offset) {
		final int count = colours.getCount();

		final Vec bestStart = new Vec(0.0f);
		final Vec bestEnd = new Vec(0.0f);
		float bestError = CompressorCluster.bestError;

		final Vec a = new Vec();
		final Vec b = new Vec();

		// prepare an ordering using the principle axis
		constructOrdering(principle, 0);

		// loop over iterations
		final int[] indices = CompressorCluster.indices;
		final int[] bestIndices = CompressorCluster.bestIndices;

		final float[] alpha = CompressorCluster.alpha;
		final float[] beta = CompressorCluster.beta;
		final float[] weights = CompressorCluster.weights;

		// check all possible clusters and iterate on the total order
		int bestIteration = 0;
		for ( int iteration = 0; ; ) {
			// first cluster [0,i) is at the start
			for ( int m = 0; m < count; ++m ) {
				indices[m] = 0;
				alpha[m] = weights[m];
				beta[m] = ZERO;
			}
			for ( int i = count; i >= 0; --i ) {
				// second cluster [i,j) is half along
				for ( int m = i; m < count; ++m ) {
					indices[m] = 2;
					alpha[m] = beta[m] = HALF * weights[m];
				}
				for ( int j = count; j >= i; --j ) {
					// last cluster [j,k) is at the end
					if ( j < count ) {
						indices[j] = 1;
						alpha[j] = ZERO;
						beta[j] = weights[j];
					}

					// solve a least squares problem to place the endpoints
					final float error = solveLeastSquares(a, b);

					// keep the solution if it wins
					if ( error < bestError ) {
						bestStart.set(a);
						bestEnd.set(b);
						System.arraycopy(indices, 0, bestIndices, 0, 16);
						bestError = error;
						bestIteration = iteration;
					}
				}
			}

			// stop if we didn't improve in this iteration
			if ( bestIteration != iteration )
				break;

			// advance if possible
			if ( ++iteration == MAX_ITERATIONS )
				break;

			// stop if a new iteration is an ordering that has already been tried
			if ( !constructOrdering(a.set(bestEnd).sub(bestStart), iteration) )
				break;
		}

		// save the block if necessary
		if ( bestError < CompressorCluster.bestError ) {
			final int[] orders = CompressorCluster.orders;
			final int[] unordered = CompressorCluster.unordered;

			// remap the indices
			final int order = 16 * bestIteration;

			for ( int i = 0; i < count; ++i )
				unordered[orders[order + i]] = bestIndices[i];
			colours.remapIndices(unordered, bestIndices);

			// save the block
			ColourBlock.writeColourBlock3(bestStart, bestEnd, bestIndices, block, offset);

			// save the error
			CompressorCluster.bestError = bestError;
		}
	}

	void compress4(final byte[] block, final int offset) {
		final int count = colours.getCount();

		final Vec bestStart = new Vec(0.0f);
		final Vec bestEnd = new Vec(0.0f);
		float bestError = CompressorCluster.bestError;

		final Vec start = new Vec();
		final Vec end = new Vec();

		// prepare an ordering using the principle axis
		constructOrdering(principle, 0);

		// check all possible clusters and iterate on the total order
		final int[] indices = CompressorCluster.indices;
		final int[] bestIndices = CompressorCluster.bestIndices;

		final float[] alpha = CompressorCluster.alpha;
		final float[] beta = CompressorCluster.beta;
		final float[] weights = CompressorCluster.weights;

		int bestIteration = 0;

		// loop over iterations
		for ( int iteration = 0; ; ) {
			// first cluster [0,i) is at the start
			for ( int m = 0; m < count; ++m ) {
				indices[m] = 0;
				alpha[m] = weights[m];
				beta[m] = ZERO;
			}
			for ( int i = count; i >= 0; --i ) {
				// second cluster [i,j) is one third along
				for ( int m = i; m < count; ++m ) {
					indices[m] = 2;
					alpha[m] = TWO_THIRDS * weights[m];
					beta[m] = ONE_THIRD * weights[m];
				}
				for ( int j = count; j >= i; --j ) {
					// third cluster [j,k) is two thirds along
					for ( int m = j; m < count; ++m ) {
						indices[m] = 3;
						alpha[m] = ONE_THIRD * weights[m];
						beta[m] = TWO_THIRDS * weights[m];
					}
					for ( int k = count; k >= j; --k ) {
						// last cluster [k,n) is at the end
						if ( k < count ) {
							indices[k] = 1;
							alpha[k] = ZERO;
							beta[k] = weights[k];
						}

						// solve a least squares problem to place the endpoints
						final float error = solveLeastSquares(start, end);

						// keep the solution if it wins
						if ( error < bestError ) {
							bestStart.set(start);
							bestEnd.set(end);
							System.arraycopy(indices, 0, bestIndices, 0, 16);
							bestError = error;
							bestIteration = iteration;
						}
					}
				}
			}

			// stop if we didn't improve in this iteration
			if ( bestIteration != iteration )
				break;

			// advance if possible
			++iteration;
			if ( iteration == MAX_ITERATIONS )
				break;

			// stop if a new iteration is an ordering that has already been tried
			if ( !constructOrdering(start.set(bestEnd).sub(bestStart), iteration) )
				break;
		}

		// save the block if necessary
		if ( bestError < CompressorCluster.bestError ) {
			final int[] orders = CompressorCluster.orders;
			final int[] unordered = CompressorCluster.unordered;

			// remap the indices
			final int order = 16 * bestIteration;
			for ( int i = 0; i < count; ++i )
				unordered[orders[order + i]] = bestIndices[i];
			colours.remapIndices(unordered, bestIndices);

			// save the block
			ColourBlock.writeColourBlock4(bestStart, bestEnd, bestIndices, block, offset);

			// save the error
			CompressorCluster.bestError = bestError;
		}
	}

	private boolean constructOrdering(final Vec axis, final int iteration) {
		// cache some values
		final int count = colours.getCount();
		final Vec[] values = colours.getPoints();

		final int[] orders = CompressorCluster.orders;

		// build the list of dot products
		final float[] dps = CompressorCluster.dps;
		final int order = 16 * iteration;
		for ( int i = 0; i < count; ++i ) {
			dps[i] = values[i].dot(axis);
			orders[order + i] = i;
		}

		// stable sort using them
		for ( int i = 0; i < count; ++i ) {
			for ( int j = i; j > 0 && dps[j] < dps[j - 1]; --j ) {
				final float tmpF = dps[j];
				dps[j] = dps[j - 1];
				dps[j - 1] = tmpF;

				final int tmpI = orders[order + j];
				orders[order + j] = orders[order + j - 1];
				orders[order + j - 1] = tmpI;
			}
		}

		// check this ordering is unique
		for ( int it = 0; it < iteration; ++it ) {
			final int prev = 16 * it;
			boolean same = true;
			for ( int i = 0; i < count; ++i ) {
				if ( orders[order + i] != orders[prev + i] ) {
					same = false;
					break;
				}
			}
			if ( same )
				return false;
		}

		// copy the ordering and weight all the points
		final Vec[] points = colours.getPoints();
		final float[] cWeights = colours.getWeights();
		xxSum.set(0.0f);

		final float[] weighted = CompressorCluster.weighted;

		for ( int i = 0, j = 0; i < count; ++i, j += 3 ) {
			final int p = orders[order + i];

			final float weight = cWeights[p];
			final Vec point = points[p];

			weights[i] = weight;

			final float wX = weight * point.x();
			final float wY = weight * point.y();
			final float wZ = weight * point.z();

			xxSum.add(wX * wX, wY * wY, wZ * wZ);

			weighted[j + 0] = wX;
			weighted[j + 1] = wY;
			weighted[j + 2] = wZ;
		}
		return true;
	}

	private float solveLeastSquares(final Vec start, final Vec end) {
		final int count = colours.getCount();

		float alpha2_sum = 0.0f;
		float beta2_sum = 0.0f;
		float alphabeta_sum = 0.0f;

		float alphax_sumX = 0f;
		float alphax_sumY = 0f;
		float alphax_sumZ = 0f;

		float betax_sumX = 0f;
		float betax_sumY = 0f;
		float betax_sumZ = 0f;

		final float[] alpha = CompressorCluster.alpha;
		final float[] beta = CompressorCluster.beta;
		final float[] weighted = CompressorCluster.weighted;

		// accumulate all the quantities we need
		for ( int i = 0, j = 0; i < count; ++i, j += 3 ) {
			final float a = alpha[i];
			final float b = beta[i];

			alpha2_sum += a * a;
			beta2_sum += b * b;
			alphabeta_sum += a * b;

			alphax_sumX += weighted[j + 0] * a;
			alphax_sumY += weighted[j + 1] * a;
			alphax_sumZ += weighted[j + 2] * a;

			betax_sumX += weighted[j + 0] * b;
			betax_sumY += weighted[j + 1] * b;
			betax_sumZ += weighted[j + 2] * b;
		}

		float aX, aY, aZ;
		float bX, bY, bZ;

		// zero where non-determinate
		if ( beta2_sum == 0.0f ) {
			final float rcp = 1.0f / alpha2_sum;

			aX = alphax_sumX * rcp;
			aY = alphax_sumY * rcp;
			aZ = alphax_sumZ * rcp;
			bX = bY = bZ = 0.0f;
		} else if ( alpha2_sum == 0.0f ) {
			final float rcp = 1.0f / beta2_sum;

			aX = aY = aZ = 0.0f;
			bX = betax_sumX * rcp;
			bY = betax_sumY * rcp;
			bZ = betax_sumZ * rcp;
		} else {
			final float rcp = 1.0f / (alpha2_sum * beta2_sum - alphabeta_sum * alphabeta_sum);
			if ( rcp == (1.0f / 0.0f) ) // Detect Infinity
				return Float.MAX_VALUE;

			aX = (alphax_sumX * beta2_sum - betax_sumX * alphabeta_sum) * rcp;
			aY = (alphax_sumY * beta2_sum - betax_sumY * alphabeta_sum) * rcp;
			aZ = (alphax_sumZ * beta2_sum - betax_sumZ * alphabeta_sum) * rcp;

			bX = (betax_sumX * alpha2_sum - alphax_sumX * alphabeta_sum) * rcp;
			bY = (betax_sumY * alpha2_sum - alphax_sumY * alphabeta_sum) * rcp;
			bZ = (betax_sumZ * alpha2_sum - alphax_sumZ * alphabeta_sum) * rcp;
		}

		// clamp the output to [0, 1]
		// clamp to the grid
		aX = clamp(aX, GRID_X, GRID_X_RCP);
		aY = clamp(aY, GRID_Y, GRID_Y_RCP);
		aZ = clamp(aZ, GRID_Z, GRID_Z_RCP);

		start.set(aX, aY, aZ);

		bX = clamp(bX, GRID_X, GRID_X_RCP);
		bY = clamp(bY, GRID_Y, GRID_Y_RCP);
		bZ = clamp(bZ, GRID_Z, GRID_Z_RCP);

		end.set(bX, bY, bZ);

		// compute the error
		final float eX = aX * aX * alpha2_sum + bX * bX * beta2_sum + xxSum.x() + 2.0f * (aX * bX * alphabeta_sum - aX * alphax_sumX - bX * betax_sumX);
		final float eY = aY * aY * alpha2_sum + bY * bY * beta2_sum + xxSum.y() + 2.0f * (aY * bY * alphabeta_sum - aY * alphax_sumY - bY * betax_sumY);
		final float eZ = aZ * aZ * alpha2_sum + bZ * bZ * beta2_sum + xxSum.z() + 2.0f * (aZ * bZ * alphabeta_sum - aZ * alphax_sumZ - bZ * betax_sumZ);

		// apply the metric to the error term
		return metric.dot(eX, eY, eZ);
	}

}