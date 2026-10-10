package jks.tools2d.parallax.editor.vue.edition;

import static jks.tools2d.parallax.editor.gvars.FVars_Extensions.JSON_PARALLAX;
import static jks.tools2d.parallax.editor.gvars.FVars_Extensions.PARALLAX;
import static jks.tools2d.parallax.editor.gvars.FVars_Extensions.PARALLAX_PROJECT;
import static jks.tools2d.libgdxutils.Utils_Interface.onChange;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;

import jks.tools2d.parallax.editor.gvars.GVars_UI;

/** Help text and links to the video tutorials. */
public class VE_Tab_Meta_Informations extends Tab
{
	private static final String TEXT = "Hello! This tool helps you build beautiful parallax backgrounds with ease."
			+ "\n\nDrag and drop a project or an export anywhere to open it"
			+ "\n(." + PARALLAX + ", ." + JSON_PARALLAX + " or ." + PARALLAX_PROJECT + ")."
			+ "\n\nTo import pictures, use an .atlas, or drag one or more .png files: no other format is accepted."
			+ "\n\nFor more information, check the tutorials with the buttons below."
			+ "\n\nFor any request, contact me at JavaKhanStudio@gmail.com";

	private final Table mainTable = new Table();
	private final VisLabel infos;
	private final VisTextButton goTutorialFr, goTutorialEng;

	VE_Tab_Meta_Informations()
	{
		super(false, false);

		infos = new VisLabel(TEXT);
		infos.setWrap(true);
		infos.setColor(Color.LIGHT_GRAY);

		goTutorialEng = linkButton("Tutorial (ENG)", "https://www.youtube.com/watch?v=FVxGCaReshc");
		goTutorialFr = linkButton("Tutoriel (FR)", "https://www.youtube.com/watch?v=AkKpn8qj_pA");
		infos.setName("infos.text");
		goTutorialEng.setName("infos.tutorialEng");
		goTutorialFr.setName("infos.tutorialFr");

		// Laid out by the table (r68), the text wrapped to the panel and the buttons a line high, both read off the font.
		float buttonHeight = GVars_UI.fontSize() * 2.5f;
		mainTable.top().pad(8);
		mainTable.add(infos).colspan(2).growX().padBottom(GVars_UI.fontSize()).row();
		mainTable.add(goTutorialEng).growX().height(buttonHeight).padRight(4);
		mainTable.add(goTutorialFr).growX().height(buttonHeight).padLeft(4);
	}

	private static VisTextButton linkButton(String text, String url)
	{
		VisTextButton button = new VisTextButton(text);
		onChange(button, () -> Gdx.net.openURI(url));
		return button;
	}

	@Override
	public String getTabTitle()
	{return "INFOS";}

	@Override
	public Table getContentTable()
	{return mainTable;}
}
