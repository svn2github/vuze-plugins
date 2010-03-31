/**
 * 
 */
package org.gudy.azureus2.countrylocator;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * This class implements a stream filter for reading compressed data in
 * the GZIP file format.
 *
 * @see		InflaterInputStream
 * @version 	1.28, 06/11/04
 * @author 	David Connelly
 *
 * @note Extended to give more details
 */
public class DetailedGZIPInputStream extends InflaterInputStream
{
	/**
	 * CRC-32 for uncompressed data.
	 */
	protected CRC32 crc = new CRC32();

	/**
	 * Indicates end of input stream.
	 */
	protected boolean eos;

	private boolean closed = false;

	private String name = "";

	private String comment = "";

	private long time = -1;

	/**
	 * Check to make sure that this stream has not been closed
	 */
	private void ensureOpen()
		throws IOException
	{
		if (closed) {
			throw new IOException("Stream closed");
		}
	}

	/**
	 * Creates a new input stream with the specified buffer size.
	 * @param in the input stream
	 * @param size the input buffer size
	 * @exception IOException if an I/O error has occurred
	 * @exception IllegalArgumentException if size is <= 0
	 */
	public DetailedGZIPInputStream(InputStream in, int size) throws IOException {
		super(in, new Inflater(true), size);
		readHeader();
		crc.reset();
	}

	/**
	 * Creates a new input stream with a default buffer size.
	 * @param in the input stream
	 * @exception IOException if an I/O error has occurred
	 */
	public DetailedGZIPInputStream(InputStream in) throws IOException {
		this(in, 512);
	}

	/**
	 * Reads uncompressed data into an array of bytes. Blocks until enough
	 * input is available for decompression.
	 * @param buf the buffer into which the data is read
	 * @param off the start offset of the data
	 * @param len the maximum number of bytes read
	 * @return	the actual number of bytes read, or -1 if the end of the
	 *		compressed input stream is reached
	 * @exception IOException if an I/O error has occurred or the compressed
	 *			      input data is corrupt
	 */
	public int read(byte[] buf, int off, int len)
		throws IOException
	{
		ensureOpen();
		if (eos) {
			return -1;
		}
		len = super.read(buf, off, len);
		if (len == -1) {
			readTrailer();
			eos = true;
		} else {
			crc.update(buf, off, len);
		}
		return len;
	}

	/**
	 * Closes this input stream and releases any system resources associated
	 * with the stream.
	 * @exception IOException if an I/O error has occurred
	 */
	public void close()
		throws IOException
	{
		if (!closed) {
			inf.end();
			super.close();
			eos = true;
			closed = true;
		}
	}

	/**
	 * GZIP header magic number.
	 */
	public final static int GZIP_MAGIC = 0x8b1f;

	/*
	 * File header flags.
	 */
	private final static int FTEXT = 1; // Extra text

	private final static int FHCRC = 2; // Header CRC

	private final static int FEXTRA = 4; // Extra field

	private final static int FNAME = 8; // File name

	private final static int FCOMMENT = 16; // File comment

	/*
	 * Reads GZIP member header.
	 */
	private void readHeader()
		throws IOException
	{
		CheckedInputStream in = new CheckedInputStream(this.in, crc);
		crc.reset();
		// Check header magic
		if (readUShort(in) != GZIP_MAGIC) {
			throw new IOException("Not in GZIP format");
		}
		// Check compression method
		if (readUByte(in) != 8) {
			throw new IOException("Unsupported compression method");
		}
		// Read flags
		int flg = readUByte(in);
		// Skip MTIME, XFL, and OS fields

		time = readUInt(in) * 1000;

		skipBytes(in, 2);
		// Skip optional extra field
		if ((flg & FEXTRA) == FEXTRA) {
			skipBytes(in, readUShort(in));
		}

		if ((flg & FNAME) == FNAME) {
			name = readString(in);
		}

		if ((flg & FCOMMENT) == FCOMMENT) {
			comment = readString(in);
		}
		// Check optional header CRC
		if ((flg & FHCRC) == FHCRC) {
			int v = (int) crc.getValue() & 0xffff;
			if (readUShort(in) != v) {
				throw new IOException("Corrupt GZIP header");
			}
		}
	}

	private String readString(InputStream in)
		throws IOException
	{
		StringBuffer sb = new StringBuffer();
		char character = (char) readUByte(in);
		while (character != 0) {
			sb.append(character);
			character = (char) readUByte(in);
		}
		;
		return sb.toString();
	}

	/*
	 * Reads GZIP member trailer.
	 */
	private void readTrailer()
		throws IOException
	{
		InputStream in = this.in;
		int n = inf.getRemaining();
		if (n > 0) {
			in = new SequenceInputStream(new ByteArrayInputStream(buf, len - n, n),
					in);
		}
		long v = crc.getValue();
		if (readUInt(in) != v || readUInt(in) != inf.getTotalOut()) {
			throw new IOException("Corrupt GZIP trailer");
		}
	}

	/*
	 /*
	 * Reads unsigned integer in Intel byte order.
	 */
	private long readUInt(InputStream in)
		throws IOException
	{
		long s = readUShort(in);
		return ((long) readUShort(in) << 16) | s;
	}

	/*
	 * Reads unsigned short in Intel byte order.
	 */
	private int readUShort(InputStream in)
		throws IOException
	{
		int b = readUByte(in);
		return (readUByte(in) << 8) | b;
	}

	/*
	 * Reads unsigned byte.
	 */
	private int readUByte(InputStream in)
		throws IOException
	{
		int b = in.read();
		if (b == -1) {
			throw new EOFException();
		}
		return b;
	}

	private byte[] tmpbuf = new byte[128];

	/*
	 * Skips bytes of input data blocking until all bytes are skipped.
	 * Does not assume that the input stream is capable of seeking.
	 */
	private void skipBytes(InputStream in, int n)
		throws IOException
	{
		while (n > 0) {
			int len = in.read(tmpbuf, 0, n < tmpbuf.length ? n : tmpbuf.length);
			if (len == -1) {
				throw new EOFException();
			}
			n -= len;
		}
	}

	/**
	 * Retrieve the name stored inside the gzip file
	 * 
	 * @return null if the gzip doesn't contain a file name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieve the name stored inside the gzip file
	 * 
	 * @return -1 if not present, othersize, milliseconds after epoch.
	 */
	public long getTime() {
		return time;
	}

	/**
	 * Retrieve the comment stored inside the gzip file
	 * 
	 * @return null if the gzip doesn't contain a file name
	 */
	public String getComment() {
		return comment;
	}
}
