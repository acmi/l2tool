/*
 * Copyright (c) 2003-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 *
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */
package acmi.l2.clientmod.l2tool.img;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * Targa image reader and writer adapted from sources of the <a href =
 * "http://java.sun.com/products/jimi/">Jimi</a> image I/O class library.
 * <p/>
 * <p/>
 * <p/>
 * Image decoder for image data stored in TGA file format.
 * Currently only the original TGA file format is supported. This is
 * because the new TGA format has data at the end of the file, getting
 * to the end of a file in an InputStream orient environment presents
 * several difficulties which are avoided at the moment.
 * <p/>
 * <p/>
 * <p/>
 * This is a simple decoder and is only setup to load a single image
 * from the input stream
 * <p/>
 * <p/>
 *
 * @author Robin Luiten
 * @author Kenneth Russell
 * @version $Revision: 1768 $
 */
public class TGAImage {
    public static final int GL_RGB = 0x1907;
    public static final int GL_RGBA = 0x1908;
    public static final int GL_BGR = 0x80E0;
    public static final int GL_BGRA = 0x80E1;

    private Header header;
    private int format;
    private int bpp;
    private ByteBuffer data;

    private TGAImage(Header header) {
        this.header = header;
    }

    /**
     * This class reads in all of the TGA image header in addition it also
     * reads in the imageID field as it is convenient to handle that here.
     *
     * @author Robin Luiten
     * @version 1.1
     */
    public static class Header {
        /**
         * Set of possible file format TGA types
         */
        public final static int TYPE_NEW = 0;
        public final static int TYPE_OLD = 1;
        public final static int TYPE_UNK = 2;               // cant rewind stream so unknown for now.

        /**
         * Set of possible image types in TGA file
         */
        public final static int NO_IMAGE = 0;               // no image data
        public final static int UCOLORMAPPED = 1;           // uncompressed color mapped image
        public final static int UTRUECOLOR = 2;             // uncompressed true color image
        public final static int UBLACKWHITE = 3;            // uncompressed black and white image
        public final static int COLORMAPPED = 9;            // compressed color mapped image
        public final static int TRUECOLOR = 10;             // compressed true color image
        public final static int BLACKWHITE = 11;            // compressed black and white image

        /**
         * Field image descriptor bitfield values definitions
         */
        public final static int ID_ATTRIBPERPIXEL = 0xF;
        public final static int ID_RIGHTTOLEFT = 0x10;
        public final static int ID_TOPTOBOTTOM = 0x20;
        public final static int ID_INTERLEAVE = 0xC0;

        /**
         * Field image descriptor / interleave values
         */
        public final static int I_NOTINTERLEAVED = 0;
        public final static int I_TWOWAY = 1;
        public final static int I_FOURWAY = 2;

        /**
         * Type of this TGA file format
         */
        private int tgaType;

        /**
         * initial TGA image data fields
         */
        private int idLength;         // byte value
        private int colorMapType;     // byte value
        private int imageType;        // byte value

        /**
         * TGA image colour map fields
         */
        private int firstEntryIndex;
        private int colorMapLength;
        private byte colorMapEntrySize;

        /**
         * TGA image specification fields
         */
        private int xOrigin;
        private int yOrigin;
        private int width;
        private int height;
        private byte pixelDepth;
        private byte imageDescriptor;

        private byte[] imageIDbuf;
        private String imageID;

        // For construction from user data
        Header() {
            tgaType = TYPE_OLD; // dont try and get footer.
        }

        Header(HelperInputStream in) throws IOException {
            tgaType = TYPE_OLD; // dont try and get footer.

            // initial header fields
            idLength = in.readUnsignedByte();
            colorMapType = in.readUnsignedByte();
            imageType = in.readUnsignedByte();

            // color map header fields
            firstEntryIndex = in.readUnsignedShort();
            colorMapLength = in.readUnsignedShort();
            colorMapEntrySize = in.readByte();

            // TGA image specification fields
            xOrigin = in.readUnsignedShort();
            yOrigin = in.readUnsignedShort();
            width = in.readUnsignedShort();
            height = in.readUnsignedShort();
            pixelDepth = in.readByte();
            imageDescriptor = in.readByte();

            if (idLength > 0) {
                imageIDbuf = new byte[idLength];
                in.read(imageIDbuf, 0, idLength);
                imageID = new String(imageIDbuf, "US-ASCII");
            }
        }

        public int tgaType() {
            return tgaType;
        }

        /**
         * initial TGA image data fields
         */
        public int idLength() {
            return idLength;
        }

        public int colorMapType() {
            return colorMapType;
        }

        public int imageType() {
            return imageType;
        }

        /**
         * TGA image colour map fields
         */
        public int firstEntryIndex() {
            return firstEntryIndex;
        }

        public int colorMapLength() {
            return colorMapLength;
        }

        public byte colorMapEntrySize() {
            return colorMapEntrySize;
        }

        /**
         * TGA image specification fields
         */
        public int xOrigin() {
            return xOrigin;
        }

        public int yOrigin() {
            return yOrigin;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }

        public byte pixelDepth() {
            return pixelDepth;
        }

        public byte imageDescriptor() {
            return imageDescriptor;
        }

        /**
         * bitfields in imageDescriptor
         */
        public byte attribPerPixel() {
            return (byte) (imageDescriptor & ID_ATTRIBPERPIXEL);
        }

        public boolean rightToLeft() {
            return ((imageDescriptor & ID_RIGHTTOLEFT) != 0);
        }

        public boolean topToBottom() {
            return ((imageDescriptor & ID_TOPTOBOTTOM) != 0);
        }

        public byte interleave() {
            return (byte) ((imageDescriptor & ID_INTERLEAVE) >> 6);
        }

        public byte[] imageIDbuf() {
            return imageIDbuf;
        }

        public String imageID() {
            return imageID;
        }

        @Override
        public String toString() {
            return "TGA Header " +
                    " id length: " + idLength +
                    " color map type: " + colorMapType +
                    " image type: " + imageType +
                    " first entry index: " + firstEntryIndex +
                    " color map length: " + colorMapLength +
                    " color map entry size: " + colorMapEntrySize +
                    " x Origin: " + xOrigin +
                    " y Origin: " + yOrigin +
                    " width: " + width +
                    " height: " + height +
                    " pixel depth: " + pixelDepth +
                    " image descriptor: " + imageDescriptor +
                    (imageIDbuf == null ? "" : (" ID String: " + imageID));
        }

        public int size() {
            return 18 + idLength;
        }

        // buf must be in little-endian byte order
        private void write(ByteBuffer buf) {
            buf.put((byte) idLength);
            buf.put((byte) colorMapType);
            buf.put((byte) imageType);
            buf.putShort((short) firstEntryIndex);
            buf.putShort((short) colorMapLength);
            buf.put(colorMapEntrySize);
            buf.putShort((short) xOrigin);
            buf.putShort((short) yOrigin);
            buf.putShort((short) width);
            buf.putShort((short) height);
            buf.put(pixelDepth);
            buf.put(imageDescriptor);
            if (idLength > 0) {
                try {
                    byte[] chars = imageID.getBytes("US-ASCII");
                    buf.put(chars);
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


    /**
     * Identifies the image type of the tga image data and loads
     * it into the JimiImage structure. This was taken from the
     * prototype and modified for the new Jimi structure
     */
    private void decodeImage(HelperInputStream dIn) throws IOException {
        switch (header.imageType()) {
            case Header.UCOLORMAPPED:
                throw new IOException("TGADecoder Uncompressed Colormapped images not supported");

            case Header.UTRUECOLOR:    // pixelDepth 15, 16, 24 and 32
                switch (header.pixelDepth) {
                    case 16:
                        throw new IOException("TGADecoder Compressed 16-bit True Color images not supported");

                    case 24:
                    case 32:
                        decodeRGBImageU24_32(dIn);
                        break;
                }
                break;

            case Header.UBLACKWHITE:
                throw new IOException("TGADecoder Uncompressed Grayscale images not supported");

            case Header.COLORMAPPED:
                throw new IOException("TGADecoder Compressed Colormapped images not supported");

            case Header.TRUECOLOR:
                switch (header.pixelDepth) {
                    case 16:
                        throw new IOException("TGADecoder Compressed 16-bit True Color images not supported");

                    case 24:
                    case 32:
                        decodeRGBImageRLE24_32(dIn);
                        break;
                }
                break;

            case Header.BLACKWHITE:
                throw new IOException("TGADecoder Compressed Grayscale images not supported");
        }
    }

    /**
     * This assumes that the body is for a 24 bit or 32 bit for a
     * RGB or ARGB image respectively.
     */
    private void decodeRGBImageU24_32(HelperInputStream dIn) throws IOException {
        setupImage24_32();

        int i;    // row index
        int y;    // output row index
        int rawWidth = header.width() * bpp;
        byte[] rawBuf = new byte[rawWidth];
        byte[] tmpData = new byte[rawWidth * header.height()];

        for (i = 0; i < header.height(); ++i) {
            dIn.readFully(rawBuf, 0, rawWidth);

            if (header.topToBottom())
                y = header.height - i - 1; // range 0 to (header.height - 1)
            else
                y = i;

            System.arraycopy(rawBuf, 0, tmpData, y * rawWidth, rawBuf.length);
        }

        if (format == GL_RGB || format == GL_RGBA)
            swapBGR(tmpData, rawWidth, header.height(), bpp);
        data = ByteBuffer.wrap(tmpData);
    }

    /**
     * This assumes that the body is for a 24 bit or 32 bit for a
     * RGB or ARGB image respectively.
     */
    private void decodeRGBImageRLE24_32(HelperInputStream dIn) throws IOException {
        setupImage24_32();

        byte[] pixel = new byte[bpp];
        int rawWidth = header.width() * bpp;
        byte[] tmpData = new byte[rawWidth * header.height()];
        int i = 0, j;
        int packet, len;
        while (i < tmpData.length) {
            packet = dIn.readUnsignedByte();
            len = (packet & 0x7F) + 1;
            if ((packet & 0x80) != 0) {
                dIn.read(pixel);
                for (j = 0; j < len; ++j)
                    System.arraycopy(pixel, 0, tmpData, i + j * bpp, bpp);
            } else
                dIn.read(tmpData, i, len * bpp);
            i += bpp * len;
        }

        if (format == GL_RGB || format == GL_RGBA)
            swapBGR(tmpData, rawWidth, header.height(), bpp);
        data = ByteBuffer.wrap(tmpData);
    }

    private void setupImage24_32() {
        bpp = header.pixelDepth / 8;
        switch (header.pixelDepth) {
            case 24:
                format = GL_BGR;
                break;
            case 32:
                format = GL_BGRA;
                break;
            default:
                assert false;
        }
    }

    private static void swapBGR(byte[] data, int bWidth, int height, int bpp) {
        byte r, b;
        int k;
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < bWidth; j += bpp) {
                k = i * bWidth + j;
                b = data[k + 0];
                r = data[k + 2];
                data[k + 0] = r;
                data[k + 2] = b;
            }
        }
    }

    /**
     * Returns the width of the image.
     */
    public int getWidth() {
        return header.width();
    }

    /**
     * Returns the height of the image.
     */
    public int getHeight() {
        return header.height();
    }

    /**
     * Returns the OpenGL format for this texture; e.g. GL.GL_BGR or GL.GL_BGRA.
     */
    public int getGLFormat() {
        return format;
    }

    /**
     * Returns the bytes per pixel
     */
    public int getBytesPerPixel() {
        return bpp;
    }

    /**
     * Returns the raw data for this texture in the correct
     * (bottom-to-top) order for calls to glTexImage2D.
     */
    public ByteBuffer getData() {
        return data;
    }

    /**
     * Reads a Targa image from the specified file.
     */
    public static TGAImage read(String filename) throws IOException {
        return read(new FileInputStream(filename));
    }

    /**
     * Reads a Targa image from the specified InputStream.
     */
    public static TGAImage read(InputStream in) throws IOException {
        HelperInputStream dIn = new HelperInputStream(new BufferedInputStream(in));

        Header header = new Header(dIn);
        TGAImage res = new TGAImage(header);
        res.decodeImage(dIn);
        return res;
    }

    /**
     * Writes the image in Targa format to the specified file name.
     */
    public void write(String filename) throws IOException {
        write(new File(filename));
    }

    /**
     * Writes the image in Targa format to the specified file.
     */
    public void write(File file) throws IOException {
        try (FileOutputStream stream = new FileOutputStream(file);
             FileChannel chan = stream.getChannel()) {
            ByteBuffer buf = ByteBuffer.allocate(header.size());
            buf.order(ByteOrder.LITTLE_ENDIAN);
            header.write(buf);
            buf.rewind();
            chan.write(buf);
            chan.write(data);
            chan.force(true);
            data.rewind();
        }
    }

    /**
     * Creates a TGAImage from data supplied by the end user. Shares
     * data with the passed ByteBuffer. Assumes the data is already in
     * the correct byte order for writing to disk, i.e., BGR or
     * BGRA.
     */
    public static TGAImage createFromData(int width,
                                          int height,
                                          boolean hasAlpha,
                                          boolean topToBottom,
                                          ByteBuffer data) {
        Header header = new Header();
        header.imageType = Header.UTRUECOLOR;
        header.width = width;
        header.height = height;
        header.pixelDepth = (byte) (hasAlpha ? 32 : 24);
        header.imageDescriptor = (byte) (topToBottom ? Header.ID_TOPTOBOTTOM : 0);
        // Note ID not supported
        TGAImage ret = new TGAImage(header);
        ret.data = data;
        return ret;
    }

    private static class HelperInputStream extends FilterInputStream{

        public HelperInputStream(InputStream in) {
            super(in);
        }

        public byte readByte() throws IOException {
            int ch = in.read();
            if (ch < 0)
                throw new EOFException();
            return (byte) (ch);
        }

        public int readUnsignedByte() throws IOException {
            return readByte() & 0xff;
        }

        public short readShort() throws IOException {
            int ch1 = in.read();
            int ch2 = in.read();
            if ((ch1 | ch2) < 0)
                throw new EOFException();
            return (short) (ch1 + (ch2 << 8));
        }

        public int readUnsignedShort() throws IOException {
            return readShort() & 0xffff;
        }

        public void readFully(byte[] b, int off, int len) throws IOException {
            if (len < 0)
                throw new IndexOutOfBoundsException();
            int n = 0;
            while (n < len) {
                int count = in.read(b, off + n, len - n);
                if (count < 0)
                    throw new EOFException();
                n += count;
            }
        }
    }
}
