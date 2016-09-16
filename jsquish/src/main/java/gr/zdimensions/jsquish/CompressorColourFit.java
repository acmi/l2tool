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

abstract class CompressorColourFit {

	protected static final Vec ONE_V = new Vec(1.0f);
	protected static final Vec ZERO_V = new Vec(0.0f);

	protected static final float GRID_X = 31.0f;
	protected static final float GRID_Y = 63.0f;
	protected static final float GRID_Z = 31.0f;

	protected static final float GRID_X_RCP = 1.0f / GRID_X;
	protected static final float GRID_Y_RCP = 1.0f / GRID_Y;
	protected static final float GRID_Z_RCP = 1.0f / GRID_Z;

	protected static final Matrix covariance = new Matrix();

	protected final ColourSet colours;
	protected final CompressionType type;

	protected CompressorColourFit(final ColourSet colours, final CompressionType type) {
		this.colours = colours;
		this.type = type;
	}

	final void compress(final byte[] block, final int offset) {
		if ( type == CompressionType.DXT1 ) {
			compress3(block, offset);
			if ( !colours.isTransparent() ) {
				compress4(block, offset);
			}
		} else
			compress4(block, offset);
	}

	abstract void compress3(byte[] block, int offset);

	abstract void compress4(byte[] block, int offset);

	protected static float clamp(final float v, final float GRID, final float GRID_RCP) {
		if ( v <= 0.0f )
			return 0.0f;
		else if ( v >= 1.0f )
			return 1.0f;

		return (int)(GRID * v + 0.5f) * GRID_RCP;
	}

}