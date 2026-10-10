package jks.tools2d.libgdxutils;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;

/**
 * Settings one per line, each its name left of its control and what goes with it right of it, the names in one
 * column: the editor's panels are laid out like the library's HTML labs (parallax r232), not a name on a line and its
 * slider under it across the panel (r67).
 */
public class JksForm extends Table
{
	public JksForm()
	{this(0);}

	/** A form whose name column is {@code nameWidth} wide (0: its widest name), to line up with another form. */
	public JksForm(float nameWidth)
	{
		// From the top: a tab's content sized to the tab is otherwise centred in it.
		top();
		defaults().padTop(2).padBottom(2);
		columnDefaults(0).left().padRight(6);
		if (nameWidth > 0)
			columnDefaults(0).width(nameWidth);
		columnDefaults(1).growX().fillX();
	}

	/** A line: {@code name}, {@code control} taking the rest of the width, then {@code extras} (buttons) side by side. */
	public VisLabel line(String name, Actor control, Actor... extras)
	{
		VisLabel label = new VisLabel(name);
		label.setAlignment(Align.left);
		line(label, control, extras);
		return label;
	}

	/** A line named by {@code label}, a label the caller keeps (to change its text, to give it a tooltip). */
	public void line(Actor label, Actor control, Actor... extras)
	{
		add(label);
		addControl(control, extras);
	}

	/** The width of the widest of {@code names} in the labels' font: a name column several forms share. */
	public static float nameWidth(String... names)
	{
		BitmapFont font = VisUI.getSkin().get(LabelStyle.class).font;
		GlyphLayout layout = new GlyphLayout();
		float widest = 0;
		for (String name : names)
		{
			layout.setText(font, name);
			widest = Math.max(widest, layout.width);
		}
		return (float) Math.ceil(widest) + 2;
	}

	/** A line whose control has no name: it starts under the controls, the name column left empty. */
	public void under(Actor control, Actor... extras)
	{
		add();
		addControl(control, extras);
	}

	/** The control and its extras in one cell: a column of extras would be as wide as the widest line's. */
	private void addControl(Actor control, Actor... extras)
	{
		Table cell = new Table();
		cell.add(control).growX();
		for (Actor extra : extras)
			cell.add(extra).padLeft(4);
		add(cell).row();
	}

	/**
	 * A line across both columns: a row of buttons or of check boxes, or a nested form. Not the name column's fixed
	 * width: a column's defaults reach a cell that starts in it, whatever its span.
	 */
	public void wide(Actor actor)
	{add(actor).colspan(2).left().fillX().minWidth(Value.minWidth).prefWidth(Value.prefWidth).maxWidth(Value.zero).row();}
}
