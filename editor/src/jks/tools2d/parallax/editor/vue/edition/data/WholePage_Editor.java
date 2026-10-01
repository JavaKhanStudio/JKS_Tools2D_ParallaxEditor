package jks.tools2d.parallax.editor.vue.edition.data;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import jks.tools2d.parallax.ParallaxLayer;
import jks.tools2d.parallax.editor.gvars.GVars_Vue_Edition;
import jks.tools2d.parallax.pages.Enum_LayerKind;
import jks.tools2d.parallax.pages.Parallax_Model;
import jks.tools2d.parallax.pages.Sequence_Segment;
import jks.tools2d.parallax.pages.WholePage_Model;

/** Page saved in a project (.plaxpj): layers may also use loose images, flagged by {@link #inside} = false. */
public class WholePage_Editor extends WholePage_Model
{
	/** For each layer of {@link #pageModel}: true when its image comes from the atlas. */
	public ArrayList<Boolean> inside;

	public WholePage_Editor()
	{
		super();
		inside = new ArrayList<>();
	}

	@Override
	protected List<ParallaxLayer> load(float worldWidth, float worldHeight, TextureAtlas atlas)
	{
		List<ParallaxLayer> layers = new ArrayList<>();
		ArrayList<Parallax_Model> models = pageModel.pageList;

		for (int i = 0; i < models.size(); )
		{
			Parallax_Model model = models.get(i);
			// An EMPTY or PARTICLES layer has no image to look for: core builds it.
			boolean fromAtlas = i >= inside.size() || inside.get(i) || model.kind == Enum_LayerKind.EMPTY || model.kind == Enum_LayerKind.PARTICLES;
			ParallaxLayer layer;
			if (fromAtlas)
				layer = buildLayer(model, atlas, worldWidth);
			else if (model.kind == Enum_LayerKind.SEQUENCE)
				layer = buildOutsideSequence(model, atlas, worldWidth);
			else
				layer = buildOutsideLayer(model, worldWidth);

			if (layer == null)
			{
				// Missing loose image: drop the layer from the model too, so layers and models stay aligned.
				Gdx.app.error("WholePage_Editor", "Image not found, layer removed: " + model.regionName);
				models.remove(i);
				if (i < inside.size())
					inside.remove(i);
				continue;
			}

			layers.add(layer);
			i++;
		}

		return layers;
	}

	protected ParallaxLayer buildOutsideLayer(Parallax_Model parallax, float worldWidth)
	{
		TextureRegion texture = GVars_Vue_Edition.outsideTextureReserve.get(parallax.regionName);
		if (texture == null)
			return null;

		ParallaxLayer layer = new ParallaxLayer(
				texture,
				true,
				worldWidth,
				parallax.parallaxScalingSpeedX, parallax.parallaxScalingSpeedY,
				parallax.sizeRatio);

		layer.setUseOriginalSize(useOriginalSize);
		layer.setUpEverything(parallax);
		return layer;
	}

	/** A SEQUENCE layer some of whose segments are loose images; null when one of those is missing. */
	protected ParallaxLayer buildOutsideSequence(Parallax_Model parallax, TextureAtlas atlas, float worldWidth)
	{
		List<TextureRegion> segments = new ArrayList<>(parallax.sequenceSegments.size());
		int[] weights = new int[parallax.sequenceSegments.size()];
		for (int i = 0; i < weights.length; i++)
		{
			Sequence_Segment segment = parallax.sequenceSegments.get(i);
			TextureRegion loose = GVars_Vue_Edition.outsideTextureReserve.get(segment.regionName);
			if (loose == null && (atlas == null || atlas.findRegion(segment.regionName) == null))
				return null;
			segments.add(loose != null ? loose : findRegion(segment.regionName, segment.regionPosition, atlas));
			weights[i] = segment.weight;
		}

		ParallaxLayer layer = ParallaxLayer.sequence(segments, weights, parallax.sequenceSeed, parallax.sequenceLength, worldWidth, parallax.sizeRatio);
		layer.setUseOriginalSize(useOriginalSize);
		layer.setUpEverything(parallax);
		return layer;
	}
}
