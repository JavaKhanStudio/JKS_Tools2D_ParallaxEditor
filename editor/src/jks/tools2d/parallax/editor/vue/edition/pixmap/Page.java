package jks.tools2d.parallax.editor.vue.edition.pixmap;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.OrderedMap;

import jks.tools2d.parallax.editor.vue.edition.pixmap.PixmapPacker.PixmapPackerRectangle;

public class Page 
{
	OrderedMap<String, PixmapPackerRectangle> rects = new OrderedMap<>();
	/** Null on a {@link PixmapPacker#layoutOnly()} packer. */
	Pixmap image;

	/** Creates a new page filled with the packer's transparent color. */
	public Page (PixmapPacker packer) {
		if (packer.layoutOnly) return;
		image = new Pixmap(packer.pageWidth, packer.pageHeight, packer.pageFormat);
		this.image.setColor(packer.transparentColor);
		this.image.fill();
	}
}
