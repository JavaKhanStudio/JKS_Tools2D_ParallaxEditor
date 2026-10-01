package jks.tools2d.parallax.editor.vue.edition;

import static jks.tools2d.parallax.editor.gvars.GVars_UI.baseSkin;
import static jks.tools2d.parallax.editor.gvars.GVars_Vue_Edition.currentlySelectedParallax;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.kotcrab.vis.ui.util.dialog.Dialogs;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.spinner.IntSpinnerModel;
import com.kotcrab.vis.ui.widget.spinner.Spinner;

import jks.tools2d.libgdxutils.Utils_Interface;
import jks.tools2d.parallax.ParallaxLayer;
import jks.tools2d.parallax.editor.gvars.GVars_UI;
import jks.tools2d.parallax.editor.gvars.GVars_Vue_Edition;
import jks.tools2d.parallax.editor.vue.edition.data.Position_Infos;
import jks.tools2d.parallax.editor.vue.edition.utils.Utils_LayerKind;
import jks.tools2d.parallax.pages.Sequence_Segment;

/**
 * A SEQUENCE layer's segments, a weight each, its seed and its cycle's length (docs/sequence-layers.md in the library).
 * The preview draws the cycle from the page's seed, as a game does that passes none; the letters under it are that
 * cycle, slot by slot.
 */
public class VE_Tab_Texture_Sequence extends Table
{
	/** Longest cycle spelled out letter by letter: past it, the start and "...". */
	private static final int CYCLE_LETTERS_SHOWN = 48;

	private final Table segmentRows = new Table();
	private final TextButton add = new TextButton("Add the image selected in Adding new", baseSkin);
	private final TextField seed = new TextField("", baseSkin);
	private final TextButton reroll = new TextButton("Re-roll", baseSkin);
	private final IntSpinnerModel lengthModel = new IntSpinnerModel(Utils_LayerKind.DEFAULT_SEQUENCE_LENGTH, 1, 4096);
	private final Spinner length = new Spinner("Cycle length", lengthModel);
	private final VisLabel repeats = new VisLabel(), cycle = new VisLabel();

	/** Called with the rebuilt layer after a segment was added, removed or moved: the tab refreshes from it. */
	private final Runnable onRebuilt;
	private boolean updating;

	public VE_Tab_Texture_Sequence(float width, Runnable onRebuilt)
	{
		this.onRebuilt = onRebuilt;

		segmentRows.setName("texture.sequence.segments");
		add.setName("texture.sequence.add");
		seed.setName("texture.sequence.seed");
		reroll.setName("texture.sequence.reroll");
		length.setName("texture.sequence.length");
		repeats.setName("texture.sequence.repeats");
		cycle.setName("texture.sequence.cycle");

		seed.setTextFieldFilter((field, c) -> Character.isDigit(c) || c == '-');
		length.setProgrammaticChangeEvents(false);
		cycle.setWrap(true);

		onChange(add, this::addSelectedImage);
		onChange(seed, () ->
		{
			try
			{applySequence(segments(), Integer.parseInt(seed.getText()), lengthModel.getValue());}
			catch (NumberFormatException e)
			{
				// "-" or "" while typing, or past an int: the layer keeps its seed until the field holds one.
			}
		});
		onChange(reroll, () ->
		{
			applySequence(segments(), Utils_LayerKind.newSeed(), lengthModel.getValue());
			update();
		});
		onChange(length, () -> applySequence(segments(), currentlySelectedParallax.getSequenceSeed(), lengthModel.getValue()));

		add(new VisLabel("Segments: image, weight")).colspan(2).row();
		add(segmentRows).colspan(2).row();
		add(add).colspan(2).pad(4).row();
		add(new VisLabel("Seed")).padRight(6);
		Table seedRow = new Table();
		seedRow.add(seed).width(width * 0.4f);
		seedRow.add(reroll).padLeft(4);
		add(seedRow).left().row();
		add(length).colspan(2).row();
		add(repeats).colspan(2).row();
		add(cycle).width(width * 0.9f).colspan(2).row();
	}

	private void onChange(com.badlogic.gdx.scenes.scene2d.Actor actor, Runnable action)
	{
		actor.addListener(Utils_Interface.changeListener(() ->
		{
			if (!updating && currentlySelectedParallax != null)
				action.run();
		}));
	}

	/** The selected layer's segments, copies: core's are replaced, never changed. */
	private static List<Sequence_Segment> segments()
	{
		List<Sequence_Segment> copies = new ArrayList<>();
		for (Sequence_Segment segment : currentlySelectedParallax.getSequenceSegments())
			copies.add(segment.copy());
		return copies;
	}

	/** Same regions, new weights, seed or length: the layer draws its cycle again in place. */
	private void applySequence(List<Sequence_Segment> segments, int newSeed, int newLength)
	{
		currentlySelectedParallax.setSequence(segments, newSeed, newLength);
		showCycle();
	}

	/** A segment added, removed or moved: a new layer, every other setting kept. */
	private void rebuild(List<TextureRegion> regions, List<Sequence_Segment> segments)
	{
		int[] weights = new int[segments.size()];
		for (int i = 0; i < weights.length; i++)
			weights[i] = segments.get(i).weight;
		ParallaxLayer layer = currentlySelectedParallax;
		GVars_Vue_Edition.replaceLayer(layer, Utils_LayerKind.withSegments(layer, regions, weights));
		onRebuilt.run();
	}

	private void addSelectedImage()
	{
		TextureRegion image = VE_Tab_TextureList_Adding.imageList == null ? null : VE_Tab_TextureList_Adding.imageList.getSelected();
		if (image == null)
		{
			Dialogs.showOKDialog(GVars_UI.mainUi, "Sequence", "Select an image in Add texture > Adding new first: it becomes the next segment.");
			return;
		}

		List<TextureRegion> regions = new ArrayList<>(currentlySelectedParallax.getTexRegion());
		List<Sequence_Segment> segments = segments();
		regions.add(image);
		segments.add(new Sequence_Segment(null, 0, 1));
		rebuild(regions, segments);
	}

	private void remove(int index)
	{
		List<TextureRegion> regions = new ArrayList<>(currentlySelectedParallax.getTexRegion());
		List<Sequence_Segment> segments = segments();
		regions.remove(index);
		segments.remove(index);
		rebuild(regions, segments);
	}

	/** Swaps segment {@code index} with the one {@code offset} away. */
	private void move(int index, int offset)
	{
		int other = index + offset;
		List<TextureRegion> regions = new ArrayList<>(currentlySelectedParallax.getTexRegion());
		List<Sequence_Segment> segments = segments();
		if (other < 0 || other >= regions.size())
			return;
		regions.add(other, regions.remove(index));
		segments.add(other, segments.remove(index));
		rebuild(regions, segments);
	}

	private void setWeight(int index, int weight)
	{
		List<Sequence_Segment> segments = segments();
		segments.get(index).weight = weight;
		applySequence(segments, currentlySelectedParallax.getSequenceSeed(), currentlySelectedParallax.getSequenceLength());
	}

	/** Refreshes every control from the selected SEQUENCE layer. */
	public void update()
	{
		ParallaxLayer layer = currentlySelectedParallax;
		updating = true;

		segmentRows.clearChildren();
		List<TextureRegion> regions = layer.getTexRegion();
		List<Sequence_Segment> segments = layer.getSequenceSegments();
		for (int i = 0; i < segments.size(); i++)
		{
			int index = i;
			String name = "texture.sequence.segment." + i;

			VisLabel title = new VisLabel(letter(i) + "  " + regionName(regions.get(i)));
			title.setName(name);
			IntSpinnerModel weightModel = new IntSpinnerModel(segments.get(i).weight, 0, 1000);
			Spinner weight = new Spinner("", weightModel);
			weight.setName(name + ".weight");
			weight.setProgrammaticChangeEvents(false);
			TextButton up = new TextButton("^", baseSkin), down = new TextButton("v", baseSkin), remove = new TextButton("x", baseSkin);
			up.setName(name + ".up");
			down.setName(name + ".down");
			remove.setName(name + ".remove");
			up.setDisabled(i == 0);
			down.setDisabled(i == segments.size() - 1);
			// A sequence chains one segment at least: the last one goes with the kind.
			remove.setDisabled(segments.size() == 1);

			onChange(weight, () -> setWeight(index, weightModel.getValue()));
			onChange(up, () -> move(index, -1));
			onChange(down, () -> move(index, +1));
			onChange(remove, () -> remove(index));

			segmentRows.add(title).left().padRight(4);
			segmentRows.add(weight);
			segmentRows.add(up).padLeft(2);
			segmentRows.add(down).padLeft(2);
			segmentRows.add(remove).padLeft(2).row();
		}

		seed.setText(Integer.toString(layer.getSequenceSeed()));
		lengthModel.setValue(layer.getSequenceLength(), false);
		showCycle();
		updating = false;
	}

	/** The cycle's slots as their segments' letters, and how many screens it scrolls before it repeats. */
	private void showCycle()
	{
		ParallaxLayer layer = currentlySelectedParallax;
		repeats.setText(String.format("repeats every %.1f screens", layer.getTotalWidth() / layer.getWorldWidth()));

		StringBuilder letters = new StringBuilder("Cycle: ");
		int shown = Math.min(layer.getSequenceLength(), CYCLE_LETTERS_SHOWN);
		for (int slot = 0; slot < shown; slot++)
			letters.append(letter(layer.getCycleSegment(slot)));
		if (shown < layer.getSequenceLength())
			letters.append("...");
		cycle.setText(letters);
	}

	/** A, B... Z, then 26, 27...: a segment's name in the cycle line. */
	private static String letter(int segment)
	{return segment < 26 ? String.valueOf((char) ('A' + segment)) : "(" + segment + ")";}

	private static String regionName(TextureRegion region)
	{
		Position_Infos info = GVars_Vue_Edition.imageRef.get(region);
		if (info == null)
			return "?";
		String name = info.fromAtlas ? info.url : info.url.substring(Math.max(info.url.lastIndexOf('/'), info.url.lastIndexOf('\\')) + 1);
		return info.position == 0 ? name : name + " #" + info.position;
	}
}
