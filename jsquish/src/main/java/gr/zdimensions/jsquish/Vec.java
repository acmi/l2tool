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

final class Vec {

	private float x;
	private float y;
	private float z;

	Vec() {
	}

	Vec(final float a) {
		this(a, a, a);
	}

	Vec(final Vec v) {
		this(v.x, v.y, v.z);
	}

	Vec(final float a, final float b, final float c) {
		x = a;
		y = b;
		z = c;
	}

	float x() { return x; }

	float y() { return y; }

	float z() { return z; }

	Vec set(final float a) {
		this.x = a;
		this.y = a;
		this.z = a;

		return this;
	}

	Vec set(final float x, final float y, final float z) {
		this.x = x;
		this.y = y;
		this.z = z;

		return this;
	}

	Vec set(final Vec v) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;

		return this;
	}

	Vec add(final Vec v) {
		x += v.x;
		y += v.y;
		z += v.z;

		return this;
	}

	Vec add(final float x, final float y, final float z) {
		this.x += x;
		this.y += y;
		this.z += z;

		return this;
	}

	Vec sub(final Vec v) {
		x -= v.x;
		y -= v.y;
		z -= v.z;

		return this;
	}

	Vec mul(final float s) {
		x *= s;
		y *= s;
		z *= s;

		return this;
	}

	Vec mul(final Vec v) {
		x *= v.x;
		y *= v.y;
		z *= v.z;

		return this;
	}

	Vec div(final float s) {
		final float t = 1.0f / s;

		x *= t;
		y *= t;
		z *= t;

		return this;
	}

	float lengthSQ() {
		return dot(this);
	}

	float dot(final Vec v) {
		return x * v.x + y * v.y + z * v.z;
	}

}