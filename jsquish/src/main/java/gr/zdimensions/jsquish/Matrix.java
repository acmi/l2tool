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

final class Matrix {

	private static final float FLT_EPSILON = 0.00001f;

	private static float[] m = new float[6];
	private static float[] u = new float[6];

	private float[] values = new float[6];

	Matrix() {
	}

	Matrix(float a) {
		for ( int i = 0; i < 6; ++i )
			values[i] = a;
	}

	float get(final int index) {
		return values[index];
	}

	static Matrix computeWeightedCovariance(final ColourSet m_colours, Matrix covariance) {
		final int count = m_colours.getCount();
		final Vec[] points = m_colours.getPoints();
		final float[] weights = m_colours.getWeights();

		final Vec centroid = new Vec();
		final Vec a = new Vec();
		final Vec b = new Vec();

		// compute the centroid
		float total = 0.0f;
		for ( int i = 0; i < count; ++i ) {
			total += weights[i];
			centroid.add(a.set(points[i]).mul(weights[i]));
		}
		centroid.div(total);

		// accumulate the covariance matrix
		if ( covariance == null )
			covariance = new Matrix();
		else
			Arrays.fill(covariance.values, 0.0f);

		final float[] values = covariance.values;

		for ( int i = 0; i < count; ++i ) {
			a.set(points[i]).sub(centroid);
			b.set(a).mul(weights[i]);

			values[0] += a.x() * b.x();
			values[1] += a.x() * b.y();
			values[2] += a.x() * b.z();
			values[3] += a.y() * b.y();
			values[4] += a.y() * b.z();
			values[5] += a.z() * b.z();
		}

		// return it
		return covariance;
	}

	private static Vec getMultiplicity1Evector(final Matrix matrix, final float evalue) {
		final float[] values = matrix.values;

		// compute M
		final float[] m = Matrix.m;
		m[0] = values[0] - evalue;
		m[1] = values[1];
		m[2] = values[2];
		m[3] = values[3] - evalue;
		m[4] = values[4];
		m[5] = values[5] - evalue;

		// compute U
		final float[] u = Matrix.u;
		u[0] = m[3] * m[5] - m[4] * m[4];
		u[1] = m[2] * m[4] - m[1] * m[5];
		u[2] = m[1] * m[4] - m[2] * m[3];
		u[3] = m[0] * m[5] - m[2] * m[2];
		u[4] = m[1] * m[2] - m[4] * m[0];
		u[5] = m[0] * m[3] - m[1] * m[1];

		// find the largest component
		float mc = abs(u[0]);
		int mi = 0;
		for ( int i = 1; i < 6; ++i ) {
			final float c = abs(u[i]);
			if ( c > mc ) {
				mc = c;
				mi = i;
			}
		}

		// pick the column with this component
		switch ( mi ) {
			case 0:
				return new Vec(u[0], u[1], u[2]);
			case 1:
			case 3:
				return new Vec(u[1], u[3], u[4]);
			default:
				return new Vec(u[2], u[4], u[5]);
		}
	}

	private static Vec getMultiplicity2Evector(final Matrix matrix, final float evalue) {
		final float[] values = matrix.values;

		// compute M
		final float[] m = Matrix.m;
		m[0] = values[0] - evalue;
		m[1] = values[1];
		m[2] = values[2];
		m[3] = values[3] - evalue;
		m[4] = values[4];
		m[5] = values[5] - evalue;

		// find the largest component
		float mc = abs(m[0]);
		int mi = 0;
		for ( int i = 1; i < 6; ++i ) {
			final float c = abs(m[i]);
			if ( c > mc ) {
				mc = c;
				mi = i;
			}
		}

		// pick the first eigenvector based on this index
		switch ( mi ) {
			case 0:
			case 1:
				return new Vec(-m[1], m[0], 0.0f);
			case 2:
				return new Vec(m[2], 0.0f, -m[0]);
			case 3:
			case 4:
				return new Vec(0.0f, -m[4], m[3]);
			default:
				return new Vec(0.0f, -m[5], m[4]);
		}
	}

	static Vec computePrincipleComponent(final Matrix matrix) {
		final float[] m = matrix.values;

		// compute the cubic coefficients
		final float c0 = m[0] * m[3] * m[5]
						 + 2.0f * m[1] * m[2] * m[4]
						 - m[0] * m[4] * m[4]
						 - m[3] * m[2] * m[2]
						 - m[5] * m[1] * m[1];
		final float c1 = m[0] * m[3] + m[0] * m[5] + m[3] * m[5]
						 - m[1] * m[1] - m[2] * m[2] - m[4] * m[4];
		final float c2 = m[0] + m[3] + m[5];

		// compute the quadratic coefficients
		final float a = c1 - (1.0f / 3.0f) * c2 * c2;
		final float b = (-2.0f / 27.0f) * c2 * c2 * c2 + (1.0f / 3.0f) * c1 * c2 - c0;

		// compute the root count check
		final float Q = 0.25f * b * b + (1.0f / 27.0f) * a * a * a;

		// test the multiplicity
		if ( FLT_EPSILON < Q ) {
			// only one root, which implies we have a multiple of the identity
			return new Vec(1.0f);
		} else if ( Q < -FLT_EPSILON ) {
			// three distinct roots
			final float theta = (float)atan2(sqrt(-Q), -0.5f * b);
			final float rho = (float)sqrt(0.25f * b * b - Q);

			final float rt = (float)pow(rho, 1.0f / 3.0f);
			final float ct = (float)cos(theta / 3.0f);
			final float st = (float)sin(theta / 3.0f);

			float l1 = (1.0f / 3.0f) * c2 + 2.0f * rt * ct;
			final float l2 = (1.0f / 3.0f) * c2 - rt * (ct + (float)sqrt(3.0f) * st);
			final float l3 = (1.0f / 3.0f) * c2 - rt * (ct - (float)sqrt(3.0f) * st);

			// pick the larger
			if ( abs(l2) > abs(l1) )
				l1 = l2;
			if ( abs(l3) > abs(l1) )
				l1 = l3;

			// get the eigenvector
			return getMultiplicity1Evector(matrix, l1);
		} else { // if( -FLT_EPSILON <= Q && Q <= FLT_EPSILON )
			// two roots
			final float rt;
			if ( b < 0.0f )
				rt = (float)-pow(-0.5f * b, 1.0f / 3.0f);
			else
				rt = (float)pow(0.5f * b, 1.0f / 3.0f);

			final float l1 = (1.0f / 3.0f) * c2 + rt;		// repeated
			final float l2 = (1.0f / 3.0f) * c2 - 2.0f * rt;

			// get the eigenvector
			if ( abs(l1) > abs(l2) )
				return getMultiplicity2Evector(matrix, l1);
			else
				return getMultiplicity1Evector(matrix, l2);
		}
	}

}