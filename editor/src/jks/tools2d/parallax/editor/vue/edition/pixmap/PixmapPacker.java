package jks.tools2d.parallax.editor.vue.edition.pixmap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Blending;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * libGDX's PixmapPacker (Apache-2.0, credited in NOTICE), trimmed to what flatten uses: named images, the guillotine
 * strategy, whitespace stripping, a border over half the padding, and {@link #layoutOnly()}. Its pages are written by
 * {@link PixmapPackerIO}, never uploaded as textures. Gone (r41): the skyline strategy, nine-patches (a packed name
 * always ends in {@code #index}, never {@code .9}), texture uploads and atlas generation.
 */
public class PixmapPacker implements Disposable
{
	boolean disposed;
	int pageWidth, pageHeight;
	Format pageFormat;
	int padding;
	boolean duplicateBorder;
	boolean stripWhitespaceX, stripWhitespaceY;
	int alphaThreshold;
	final Color transparentColor = new Color(0f, 0f, 0f, 0f);
	final Array<Page> pages = new Array<>();
	PackStrategy packStrategy;
	/** Places rectangles without pages' pixels: see {@link #layoutOnly()}. */
	boolean layoutOnly;

	/** @see PixmapPacker#PixmapPacker(int, int, Format, int, boolean, boolean, boolean, PackStrategy) */
	public PixmapPacker (int pageWidth, int pageHeight, Format pageFormat, int padding, boolean duplicateBorder, PackStrategy packStrategy) {
		this(pageWidth, pageHeight, pageFormat, padding, duplicateBorder, false, false, packStrategy);
	}

	/** Creates a new ImagePacker which will insert all supplied pixmaps into one or more <code>pageWidth</code> by
	 * <code>pageHeight</code> pixmaps using the specified strategy.
	 * @param padding the number of blank pixels to insert between pixmaps.
	 * @param duplicateBorder duplicate the border pixels of the inserted images to avoid seams when rendering with bi-linear
	 *           filtering on.
	 * @param stripWhitespaceX strip whitespace in x axis
	 * @param stripWhitespaceY strip whitespace in y axis
	 *           */
	public PixmapPacker (int pageWidth, int pageHeight, Format pageFormat, int padding, boolean duplicateBorder, boolean stripWhitespaceX,
		boolean stripWhitespaceY, PackStrategy packStrategy) {
		this.pageWidth = pageWidth;
		this.pageHeight = pageHeight;
		this.pageFormat = pageFormat;
		this.padding = padding;
		this.duplicateBorder = duplicateBorder;
		this.stripWhitespaceX = stripWhitespaceX;
		this.stripWhitespaceY = stripWhitespaceY;
		this.packStrategy = packStrategy;
	}

	/**
	 * Makes this packer only place rectangles: its pages allocate no pixmap and {@link #pack(String, Pixmap)} copies
	 * nothing, so trying a page size costs no page memory. Call before packing anything.
	 */
	public PixmapPacker layoutOnly () {
		layoutOnly = true;
		return this;
	}

	/** Places a {@code width} x {@code height} rectangle, whitespace already stripped, on a {@link #layoutOnly()} packer. */
	public synchronized Rectangle packLayout (String name, int width, int height) {
		if (!layoutOnly) throw new GdxRuntimeException("packLayout needs a layoutOnly packer");
		if (getRect(name) != null) throw new GdxRuntimeException("Pixmap has already been packed with name: " + name);
		PixmapPackerRectangle rect = new PixmapPackerRectangle(0, 0, width, height);
		if (width > pageWidth || height > pageHeight) throw new GdxRuntimeException("Page size too small for pixmap: " + name);
		Page page = packStrategy.pack(this, name, rect);
		page.rects.put(name, rect);
		return rect;
	}

	/** Inserts the pixmap. You can later retrieve the image's position in the output image via {@link #getRect(String)}.
	 * @return Rectangle describing the area the pixmap was rendered to.
	 * @throws GdxRuntimeException in case the image did not fit due to the page size being too small or providing a duplicate
	 *            name. */
	public synchronized Rectangle pack (String name, Pixmap image) {
		if (disposed) return null;
		if (getRect(name) != null)
			throw new GdxRuntimeException("Pixmap has already been packed with name: " + name);

		PixmapPackerRectangle rect;
		Pixmap pixmapToDispose = null;
		if (stripWhitespaceX || stripWhitespaceY) {
			int originalWidth = image.getWidth();
			int originalHeight = image.getHeight();
			int[] bounds = opaqueBounds(image);
			int left = bounds[0], top = bounds[1];
			int newWidth = bounds[2] - left;
			int newHeight = bounds[3] - top;

			pixmapToDispose = new Pixmap(newWidth, newHeight, image.getFormat());
			pixmapToDispose.drawPixmap(image, 0, 0, left, top, newWidth, newHeight);
			image = pixmapToDispose;

			rect = new PixmapPackerRectangle(0, 0, newWidth, newHeight, left, top, originalWidth, originalHeight);
		} else {
			rect = new PixmapPackerRectangle(0, 0, image.getWidth(), image.getHeight());
		}

		if (rect.getWidth() > pageWidth || rect.getHeight() > pageHeight)
			throw new GdxRuntimeException("Page size too small for pixmap: " + name);

		Page page = packStrategy.pack(this, name, rect);
		page.rects.put(name, rect);

		if (!layoutOnly) {
			page.image.setBlending(Blending.None);
			page.image.drawPixmap(image, (int)rect.x, (int)rect.y);
			if (duplicateBorder) drawBorder(page.image, image, rect);
		}

		if (pixmapToDispose != null) {
			pixmapToDispose.dispose();
		}

		return rect;
	}

	/** The opaque part of {@code image} on the axes this packer strips, as {left, top, right, bottom}: the whole image when
	 * nothing is opaque, as a 0px pixmap cannot be created. */
	private int[] opaqueBounds (Pixmap image) {
		int top = 0;
		int bottom = image.getHeight();
		if (stripWhitespaceY) {
			outer:
			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth(); x++) {
					int pixel = image.getPixel(x, y);
					int alpha = ((pixel & 0x000000ff));
					if (alpha > alphaThreshold) break outer;
				}
				top++;
			}
			outer:
			for (int y = image.getHeight(); --y >= top;) {
				for (int x = 0; x < image.getWidth(); x++) {
					int pixel = image.getPixel(x, y);
					int alpha = ((pixel & 0x000000ff));
					if (alpha > alphaThreshold) break outer;
				}
				bottom--;
			}
		}
		int left = 0;
		int right = image.getWidth();
		if (stripWhitespaceX) {
			outer:
			for (int x = 0; x < image.getWidth(); x++) {
				for (int y = top; y < bottom; y++) {
					int pixel = image.getPixel(x, y);
					int alpha = ((pixel & 0x000000ff));
					if (alpha > alphaThreshold) break outer;
				}
				left++;
			}
			outer:
			for (int x = image.getWidth(); --x >= left;) {
				for (int y = top; y < bottom; y++) {
					int pixel = image.getPixel(x, y);
					int alpha = ((pixel & 0x000000ff));
					if (alpha > alphaThreshold) break outer;
				}
				right--;
			}
		}

		if (right <= left || bottom <= top)
			return new int[] {0, 0, image.getWidth(), image.getHeight()};
		return new int[] {left, top, right, bottom};
	}

	/**
	 * Local change to libGDX's packer: the border fills half the padding, the most it can without reaching a neighbour,
	 * instead of 1px. Mipmap levels average blocks of 2^n pixels: a thinner border lets them blend in the transparent
	 * padding, and a tiled layer shows a seam at every join and along its edges (r53).
	 */
	private void drawBorder (Pixmap pageImage, Pixmap image, Rectangle rect) {
		int rectX = (int)rect.x, rectY = (int)rect.y, rectWidth = (int)rect.width, rectHeight = (int)rect.height;
		int b = Math.max(1, padding / 2);
		int imageWidth = image.getWidth(), imageHeight = image.getHeight();
		// Copy corner pixels to fill corners of the padding.
		pageImage.drawPixmap(image, 0, 0, 1, 1, rectX - b, rectY - b, b, b);
		pageImage.drawPixmap(image, imageWidth - 1, 0, 1, 1, rectX + rectWidth, rectY - b, b, b);
		pageImage.drawPixmap(image, 0, imageHeight - 1, 1, 1, rectX - b, rectY + rectHeight, b, b);
		pageImage.drawPixmap(image, imageWidth - 1, imageHeight - 1, 1, 1, rectX + rectWidth, rectY + rectHeight, b, b);
		// Copy edge pixels into padding.
		pageImage.drawPixmap(image, 0, 0, imageWidth, 1, rectX, rectY - b, rectWidth, b);
		pageImage.drawPixmap(image, 0, imageHeight - 1, imageWidth, 1, rectX, rectY + rectHeight, rectWidth, b);
		pageImage.drawPixmap(image, 0, 0, 1, imageHeight, rectX - b, rectY, b, rectHeight);
		pageImage.drawPixmap(image, imageWidth - 1, 0, 1, imageHeight, rectX + rectWidth, rectY, b, rectHeight);
	}

	/** @return the {@link Page} instances created so far. If multiple threads are accessing the packer, iterating over the pages
	 *         must be done only after synchronizing on the packer. */
	public Array<Page> getPages () {
		return pages;
	}

	/** @param name the name of the image
	 * @return the rectangle for the image in the page it's stored in or null */
	public synchronized Rectangle getRect (String name) {
		for (Page page : pages) {
			Rectangle rect = page.rects.get(name);
			if (rect != null) return rect;
		}
		return null;
	}

	/** Disposes the page pixmaps. */
	public synchronized void dispose () {
		for (Page page : pages) {
			if (page.image != null) {
				page.image.dispose();
			}
		}
		disposed = true;
	}

	/** Choose the page and location for each rectangle.
	 * @author Nathan Sweet */
	static public interface PackStrategy {
		/** Returns the page the rectangle should be placed in and modifies the specified rectangle position. */
		public Page pack (PixmapPacker packer, String name, Rectangle rect);
	}

	/** Does bin packing by inserting to the right or below previously packed rectangles. This is good at packing arbitrarily sized
	 * images.
	 * @author mzechner
	 * @author Nathan Sweet
	 * @author Rob Rendell */
	static public class GuillotineStrategy implements PackStrategy {
		public Page pack (PixmapPacker packer, String name, Rectangle rect) {
			GuillotinePage page;
			if (packer.pages.size == 0) {
				// Add a page if empty.
				page = new GuillotinePage(packer);
				packer.pages.add(page);
			} else {
				// Always try to pack into the last page.
				page = (GuillotinePage)packer.pages.peek();
			}

			int padding = packer.padding;
			rect.width += padding;
			rect.height += padding;
			Node node = insert(page.root, rect);
			if (node == null) {
				// Didn't fit, pack into a new page.
				page = new GuillotinePage(packer);
				packer.pages.add(page);
				node = insert(page.root, rect);
				if (node == null) throw new GdxRuntimeException("Page size too small for a " + (int)rect.width + "x" + (int)rect.height
					+ " image with " + padding + "px padding: " + name);
			}
			node.full = true;
			rect.set(node.rect.x, node.rect.y, node.rect.width - padding, node.rect.height - padding);
			return page;
		}

		private Node insert (Node node, Rectangle rect) {
			if (!node.full && node.leftChild != null && node.rightChild != null) {
				Node newNode = insert(node.leftChild, rect);
				if (newNode == null) newNode = insert(node.rightChild, rect);
				return newNode;
			} else {
				if (node.full) return null;
				if (node.rect.width == rect.width && node.rect.height == rect.height) return node;
				if (node.rect.width < rect.width || node.rect.height < rect.height) return null;

				node.leftChild = new Node();
				node.rightChild = new Node();

				int deltaWidth = (int)node.rect.width - (int)rect.width;
				int deltaHeight = (int)node.rect.height - (int)rect.height;
				if (deltaWidth > deltaHeight) {
					node.leftChild.rect.x = node.rect.x;
					node.leftChild.rect.y = node.rect.y;
					node.leftChild.rect.width = rect.width;
					node.leftChild.rect.height = node.rect.height;

					node.rightChild.rect.x = node.rect.x + rect.width;
					node.rightChild.rect.y = node.rect.y;
					node.rightChild.rect.width = node.rect.width - rect.width;
					node.rightChild.rect.height = node.rect.height;
				} else {
					node.leftChild.rect.x = node.rect.x;
					node.leftChild.rect.y = node.rect.y;
					node.leftChild.rect.width = node.rect.width;
					node.leftChild.rect.height = rect.height;

					node.rightChild.rect.x = node.rect.x;
					node.rightChild.rect.y = node.rect.y + rect.height;
					node.rightChild.rect.width = node.rect.width;
					node.rightChild.rect.height = node.rect.height - rect.height;
				}

				return insert(node.leftChild, rect);
			}
		}

		static final class Node {
			public Node leftChild;
			public Node rightChild;
			public final Rectangle rect = new Rectangle();
			public boolean full;
		}

		static class GuillotinePage extends Page {
			Node root;

			public GuillotinePage (PixmapPacker packer) {
				super(packer);
				root = new Node();
				root.rect.x = packer.padding;
				root.rect.y = packer.padding;
				root.rect.width = packer.pageWidth - packer.padding * 2;
				root.rect.height = packer.pageHeight - packer.padding * 2;
			}
		}
	}

	public static class PixmapPackerRectangle extends Rectangle {
		int offsetX, offsetY;
		int originalWidth, originalHeight;

		PixmapPackerRectangle (int x, int y, int width, int height) {
			super(x, y, width, height);
			this.offsetX = 0;
			this.offsetY = 0;
			this.originalWidth = width;
			this.originalHeight = height;
		}

		PixmapPackerRectangle (int x, int y, int width, int height, int left, int top, int originalWidth, int originalHeight) {
			super(x, y, width, height);
			this.offsetX = left;
			this.offsetY = top;
			this.originalWidth = originalWidth;
			this.originalHeight = originalHeight;
		}
	}

}
