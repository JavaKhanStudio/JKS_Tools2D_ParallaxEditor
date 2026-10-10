package jks.tools2d.parallax.editor.vue.edition;

import static jks.tools2d.parallax.editor.gvars.GVars_Vue_Edition.projectDatas;
import static jks.tools2d.parallax.editor.gvars.GVars_Vue_Edition.projectInfos;
import static jks.tools2d.libgdxutils.Utils_Interface.onChange;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.kotcrab.vis.ui.widget.Tooltip;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;

import jks.tools2d.libgdxutils.Utils_Interface;
import jks.tools2d.parallax.editor.gvars.GVars_UI;
import jks.tools2d.parallax.editor.vue.edition.utils.Utils_Saving;
import jks.tools2d.parallax.editor.vue.edition.utils.Utils_Texture;

/** Top bar: project folder and name, save project / export buttons and export formats. */
public class VE_Options extends Table
{
	private static final float decal = 3;
	private static final float buttonSize = 50;
	private static final float textHeight = 22;

	public static TextField parallaxPath, parallaxName;
	public static VisCheckBox formatLibGDX, formatJson, forceExport, pixelArt, etc2;

	public VE_Options()
	{
		float textWidth = Gdx.graphics.getWidth() / 4f;
		float pathWidth = textWidth * 2;

		ImageButton savingProject = Utils_Interface.buildSquareButton("editor/interfaces/saveProject.png", buttonSize);
		onChange(savingProject, () -> Utils_Saving.saving_Parallax_Project(parallaxPath.getText(), parallaxName.getText(), true));

		ImageButton savingExport = Utils_Interface.buildSquareButton("editor/interfaces/saveParallax.png", buttonSize);
		onChange(savingExport, () -> Utils_Saving.saving_Parallax(parallaxPath.getText(), parallaxName.getText()));

		savingProject.setName("options.saveProject");
		savingExport.setName("options.export");

		formatLibGDX = new VisCheckBox("LibGDX");
		formatLibGDX.setChecked(true);
		formatJson = new VisCheckBox("JSON");
		forceExport = new VisCheckBox("F.Export");
		formatLibGDX.setName("options.formatLibgdx");
		formatJson.setName("options.formatJson");
		forceExport.setName("options.forceExport");
		// Stored in the project: rebuilt after a resize, the box reads it back.
		pixelArt = new VisCheckBox("Pixel art");
		pixelArt.setName("options.pixelArt");
		pixelArt.setChecked(projectDatas.pixelArt);
		new Tooltip.Builder("Export sharp: Nearest filtering, no mipmaps.\nThe preview shows it as soon as it is ticked.").target(pixelArt).build();
		onChange(pixelArt, () ->
		{
			projectDatas.pixelArt = pixelArt.isChecked();
			Utils_Texture.applyPixelArt();
		});

		// Stored in the project, as Pixel art.
		etc2 = new VisCheckBox("ETC2");
		etc2.setName("options.etc2");
		etc2.setChecked(projectDatas.etc2);
		new Tooltip.Builder("Also export the atlas as ETC2 (name.etc2.atlas):\na quarter of the video memory on OpenGL ES 3 phones.").target(etc2).build();
		onChange(etc2, () -> projectDatas.etc2 = etc2.isChecked());

		parallaxPath = new TextField("", GVars_UI.baseSkin)
		{
			@Override
			public float getPrefWidth()
			{return super.getPrefWidth() * 3;}
		};
		parallaxName = new TextField("", GVars_UI.baseSkin);
		parallaxPath.setName("options.path");
		parallaxName.setName("options.name");

		// The formats two a column, three rows and the title in the two buttons' height, at the right edge; the buttons
		// left of them, placed from the table's width: the font grows with the window (r67), a fixed 100 px did not hold it.
		Table formatTable = new Table();
		formatTable.top().left().defaults().height((buttonSize + decal) * 2 / 4f).left().padRight(8);
		formatTable.add(new VisLabel("Export formats")).colspan(2).row();
		formatTable.add(formatLibGDX);
		formatTable.add(etc2).row();
		formatTable.add(formatJson);
		formatTable.add(pixelArt).row();
		formatTable.add(forceExport).row();
		float formatWidth = formatTable.getPrefWidth();
		float buttonsX = Gdx.graphics.getWidth() - formatWidth - buttonSize - decal * 2;
		savingProject.setBounds(buttonsX, Gdx.graphics.getHeight() - (buttonSize + decal), buttonSize, buttonSize);
		savingExport.setBounds(buttonsX, Gdx.graphics.getHeight() - (buttonSize + decal) * 2, buttonSize, buttonSize);
		formatTable.setBounds(buttonsX + buttonSize + decal, savingExport.getY(), formatWidth, (buttonSize + decal) * 2);

		Table projectPathTable = new Table();
		projectPathTable.setBounds(savingProject.getX() - pathWidth - decal, savingProject.getY() + textHeight / 2 - decal, pathWidth, textHeight);
		projectPathTable.add(new VisLabel("Project Path : "));
		projectPathTable.add(parallaxPath);

		Table projectNameTable = new Table();
		projectNameTable.setBounds(savingExport.getX() - textWidth - decal, savingExport.getY() + textHeight / 2 - decal, textWidth, textHeight);
		projectNameTable.add(new VisLabel("Project Name : "));
		projectNameTable.add(parallaxName).right();

		setInfos();

		addActor(savingProject);
		addActor(savingExport);
		addActor(projectPathTable);
		addActor(projectNameTable);
		addActor(formatTable);
	}

	public void setInfos()
	{
		parallaxPath.setText(projectInfos.projectPath);
		parallaxName.setText(projectInfos.projectName);
	}
}
