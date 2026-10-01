package jks.tools2d.parallax.editor.vue.edition.utils;

import static jks.tools2d.parallax.editor.vue.Vue_Edition.parallax_Heart;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.kotcrab.vis.ui.util.dialog.Dialogs;

import jks.tools2d.parallax.EffectSupport;
import jks.tools2d.parallax.EffectSupport.Engine;
import jks.tools2d.parallax.ParallaxLayer;
import jks.tools2d.parallax.ParallaxParticles;
import jks.tools2d.parallax.editor.gvars.EditorPaths;
import jks.tools2d.parallax.editor.gvars.GVars_UI;
import jks.tools2d.parallax.editor.gvars.GVars_Vue_Edition;
import jks.tools2d.parallax.editor.vue.edition.VE_Tab_TextureList_Adding;
import jks.tools2d.parallax.editor.vue.edition.data.Position_Infos;
import jks.tools2d.parallax.heart.Gvars_Parallax;
import jks.tools2d.parallax.pages.Enum_LayerKind;
import jks.tools2d.parallax.pages.Parallax_Model;
import jks.tools2d.parallax.pages.Sequence_Segment;
import jks.tools2d.parallax.pages.Utils_Page;

/**
 * A layer's kind (docs/effect-layers.md in the library): changing it, a PARTICLES layer's effect files, and which engine
 * draws what of it, read from core's {@link EffectSupport}.
 */
public final class Utils_LayerKind
{
	/** The image a layer drew before it became EMPTY or PARTICLES, given back if it draws one again. */
	private static final Map<ParallaxLayer, TextureRegion> imageBefore = new WeakHashMap<>();
	/** A new SEQUENCE layer's cycle length: the format's default. */
	public static final int DEFAULT_SEQUENCE_LENGTH = new Parallax_Model().sequenceLength;
	private static final Random SEEDS = new Random();

	private Utils_LayerKind()
	{}

	/**
	 * A new layer of {@code kind} with every setting of {@code layer}: its kind is final in core. Null when {@code kind}
	 * draws an image and none is at hand (said in a dialog). A layer made SEQUENCE chains its one image, at weight 1,
	 * from a new seed; one made something else from a SEQUENCE keeps its first segment.
	 */
	public static ParallaxLayer withKind(ParallaxLayer layer, Enum_LayerKind kind)
	{
		Parallax_Model model = Utils_Page.buildFromPage(layer, null, 0);
		model.kind = kind;
		TextureRegion image = layer.drawsImage() ? GVars_Vue_Edition.imageOf(layer) : imageBefore.get(layer);

		ParallaxLayer rebuilt;
		switch (kind)
		{
			case EMPTY:
				rebuilt = ParallaxLayer.empty(model.name, model.sizeRatio);
				break;
			case PARTICLES:
				rebuilt = ParallaxLayer.particles(null, model.particlesAnchor, model.sizeRatio);
				break;
			case IMAGE:
			case SHADER:
			case SEQUENCE:
				if (image == null || !GVars_Vue_Edition.allImage.contains(image))
					image = VE_Tab_TextureList_Adding.imageList == null ? null : VE_Tab_TextureList_Adding.imageList.getSelected();
				if (image == null)
				{
					Dialogs.showOKDialog(GVars_UI.mainUi, "Layer kind", "A " + kind + " layer draws an image, and this one has none."
							+ "\nSelect an image in Add texture > Adding new, then choose " + kind + " again.");
					return null;
				}
				List<TextureRegion> regions = new ArrayList<>(1);
				regions.add(image);
				if (kind == Enum_LayerKind.SEQUENCE)
				{
					model.sequenceSegments.clear();
					model.sequenceSegments.add(new Sequence_Segment(null, 0, 1));
					model.sequenceSeed = newSeed();
					model.sequenceLength = DEFAULT_SEQUENCE_LENGTH;
					rebuilt = ParallaxLayer.sequence(regions, new int[] { 1 }, model.sequenceSeed, model.sequenceLength,
							Gvars_Parallax.getWorldWidth(), model.sizeRatio);
				}
				else
					rebuilt = new ParallaxLayer(kind, regions, true, Gvars_Parallax.getWorldWidth(),
							model.parallaxScalingSpeedX, model.parallaxScalingSpeedY, model.sizeRatio);
				rebuilt.setUseOriginalSize(parallax_Heart.currentPage.useOriginalSize);
				break;
			default:
				return null;
		}

		rebuilt.setUpEverything(model);
		if (!rebuilt.drawsImage() && image != null)
			imageBefore.put(rebuilt, image);
		if (kind == Enum_LayerKind.PARTICLES)
			loadLibgdxEffect(rebuilt);
		return rebuilt;
	}

	/**
	 * A copy of the SEQUENCE layer {@code layer} chaining {@code regions}, one weight each, with every other setting of
	 * it: core's cycle is sized for its regions, so a segment added, removed or moved means a new layer.
	 */
	public static ParallaxLayer withSegments(ParallaxLayer layer, List<TextureRegion> regions, int[] weights)
	{
		Parallax_Model model = Utils_Page.buildFromPage(layer, null, 0);
		model.sequenceSegments.clear();
		for (int i = 0; i < regions.size(); i++)
		{
			Position_Infos info = GVars_Vue_Edition.imageRef.get(regions.get(i));
			model.sequenceSegments.add(new Sequence_Segment(info == null ? null : info.url, info == null ? 0 : info.position, weights[i]));
		}

		ParallaxLayer rebuilt = ParallaxLayer.sequence(new ArrayList<>(regions), weights, model.sequenceSeed, model.sequenceLength,
				Gvars_Parallax.getWorldWidth(), model.sizeRatio);
		rebuilt.setUseOriginalSize(layer.isUseOriginalSize());
		rebuilt.setUpEverything(model);
		return rebuilt;
	}

	/** A seed for a new cycle: any int, 0 included (core starts xorshift elsewhere for it). */
	public static int newSeed()
	{return SEEDS.nextInt();}

	/** The folder a page's effect files are named from: its atlas's. Null for a page without an atlas. */
	public static Path effectFolder()
	{
		Path atlas = EditorPaths.atlasFile();
		return atlas == null ? null : atlas.getParent();
	}

	/** {@code file} as the page stores it: relative to {@link #effectFolder()}, with forward slashes. */
	public static String pagePath(Path file)
	{
		Path folder = effectFolder();
		Path stored = folder == null ? file : folder.relativize(file.toAbsolutePath().normalize());
		return stored.toString().replace('\\', '/');
	}

	/** Whether the effect file a page names is there, beside its atlas. */
	public static boolean exists(String pagePath)
	{
		Path folder = effectFolder();
		return pagePath != null && !pagePath.isEmpty() && folder != null && Files.isRegularFile(folder.resolve(pagePath));
	}

	/**
	 * Plays the layer's libGDX effect in the preview, loaded from the .p its particlesLibgdx names, its images from the
	 * page's atlas, or plays none. Returns why it plays none, null when it plays one or the layer names none.
	 */
	public static String loadLibgdxEffect(ParallaxLayer layer)
	{
		layer.setParticles(null);
		String path = layer.getParticlesLibgdx();
		if (path == null || path.isEmpty())
			return null;
		if (effectFolder() == null || GVars_Vue_Edition.atlas == null)
			return "the page has no atlas: its effects are found beside it";
		if (!exists(path))
			return "not found beside the atlas";

		try
		{
			ParallaxParticles effect = new ParallaxParticles();
			effect.load(new FileHandle(effectFolder().resolve(path).toFile()), GVars_Vue_Edition.atlas);
			layer.setParticles(effect);
			return null;
		}
		catch (RuntimeException e)
		{
			// ParticleEffect throws when an image the effect names is not in the atlas.
			return e.getMessage() == null ? e.toString() : e.getMessage();
		}
	}

	/** Whether {@code engine} draws all of {@code layer}, part of it, or nothing. */
	public enum Mark
	{
		DRAWN, MISSING, NOT_DRAWN
	}

	public static Mark mark(ParallaxLayer layer, Engine engine)
	{
		if (EffectSupport.whyNot(layer.kind, engine) != null)
			return Mark.NOT_DRAWN;
		return why(layer, engine) == null ? Mark.DRAWN : Mark.MISSING;
	}

	/** For a person: why {@code engine} does not draw all of {@code layer}; null when it does. */
	public static String why(ParallaxLayer layer, Engine engine)
	{
		String notDrawn = EffectSupport.whyNot(layer.kind, engine);
		if (notDrawn != null)
			return notDrawn;

		switch (layer.kind)
		{
			case EMPTY:
				return layer.getName() == null || layer.getName().isEmpty() ? "no name: no game hook can be registered for it" : null;
			case PARTICLES:
				if (engine == Engine.GODOT)
					return blank(layer.getParticlesGodot()) ? "no Godot scene (.tscn): Godot draws nothing here" : null;
				return blank(layer.getParticlesLibgdx()) ? "no libGDX effect (.p): libGDX draws nothing here" : null;
			case SHADER:
				return EffectSupport.whyNot(layer.getShaderEffect(), engine);
			default:
				return null;
		}
	}

	private static boolean blank(String text)
	{return text == null || text.isEmpty();}
}
