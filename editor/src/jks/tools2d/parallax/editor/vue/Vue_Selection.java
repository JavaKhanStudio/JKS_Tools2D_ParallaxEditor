package jks.tools2d.parallax.editor.vue;

import static jks.tools2d.parallax.editor.gvars.FVars_Extensions.ATLAS;
import static jks.tools2d.parallax.editor.gvars.FVars_Extensions.JSON_PARALLAX;
import static jks.tools2d.parallax.editor.gvars.FVars_Extensions.PARALLAX;
import static jks.tools2d.parallax.editor.gvars.FVars_Extensions.PARALLAX_PROJECT;
import static jks.tools2d.parallax.editor.gvars.GVars_UI.baseSkin;
import static jks.tools2d.parallax.editor.gvars.GVars_Vue_Edition.projectDatas;
import static jks.tools2d.parallax.editor.gvars.GVars_Vue_Edition.projectInfos;
import static jks.tools2d.parallax.editor.gvars.GVars_Vue_Edition.relativePath;
import static jks.tools2d.libgdxutils.Utils_Interface.onChange;

import java.io.File;
import java.io.FileFilter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.kotcrab.vis.ui.util.dialog.Dialogs;

import jks.tools2d.filechooser.FC_List;
import jks.tools2d.filechooser.FileChooser_Listener;
import jks.tools2d.libgdxutils.Utils_Scene2D;
import jks.tools2d.parallax.editor.gvars.EditorPaths;
import jks.tools2d.parallax.editor.gvars.GVars_Heart_Editor;
import jks.tools2d.parallax.editor.gvars.GVars_Serialization_Editor;
import jks.tools2d.parallax.editor.gvars.GVars_UI;
import jks.tools2d.parallax.editor.vue.edition.data.Project_Data;
import jks.tools2d.parallax.editor.vue.edition.data.Project_Infos;
import jks.tools2d.parallax.editor.vue.model.AVue_Model;
import jks.tools2d.parallax.pages.Utils_Page;
import jks.tools2d.parallax.pages.WholePage_Model;

/** Start screen: pick a project, a parallax or an atlas, or start an empty project. */
public class Vue_Selection extends AVue_Model
{
	private static final float sizeMultChooser = 0.7f;

	private FC_List chooser;
	private TextButton createNew;
	private Label title;

	@Override
	public void init()
	{
		// Back from the edition screen, or opened in a window resized before: the font follows the window.
		GVars_UI.fitWindow();
		FileHandle filesRoot = Gdx.files.absolute(EditorPaths.filesRoot().toString());

		chooser = new FC_List(baseSkin, new FileChooser_Listener()
		{
			@Override
			public void choose(FileHandle file)
			{selectSingleFile(file);}

			@Override
			public void choose(Array<FileHandle> files)
			{}

			@Override
			public void cancel()
			{} // no cancel button here
		});
		chooser.setFileFilter(buildFileFilter());
		// The start screen has nothing to cancel back to.
		chooser.setCancelable(false);
		chooser.setStartDirectory(filesRoot);

		chooser.setName("selection.chooser");

		createNew = new TextButton("NEW", baseSkin);
		createNew.setName("selection.new");
		onChange(createNew, () ->
		{
			projectInfos = new Project_Infos();
			projectInfos.projectName = "newProject";
			projectInfos.projectPath = filesRoot.path();
			projectDatas = new Project_Data();
			relativePath = projectInfos.projectPath;
			GVars_Heart_Editor.changeVue(new Vue_Edition(), true);
		});

		title = new Label("Open a project (." + PARALLAX_PROJECT + "), a parallax (." + PARALLAX + " / ." + JSON_PARALLAX + ")"
				+ "\nor an atlas (." + ATLAS + ") to create a new project from it", baseSkin);
		// Tinted, not a copied style: a copy keeps the font a resize disposes (GVars_UI.resize swaps the skin's own).
		title.setColor(Color.LIGHT_GRAY);
		title.setAlignment(Align.center);
		layout();

		GVars_UI.mainUi.addActor(createNew);
		GVars_UI.mainUi.addActor(chooser);
		GVars_UI.mainUi.addActor(title);
		// The chooser's arrow, backspace and Enter keys only reach it while it or a child holds the focus.
		GVars_UI.mainUi.setKeyboardFocus(chooser);
	}

	/** Places the chooser, NEW and the title from the window's size: at init and after every resize (r68). */
	private void layout()
	{
		float width = Gdx.graphics.getWidth(), height = Gdx.graphics.getHeight();
		chooser.setSize(width * sizeMultChooser, height * sizeMultChooser);
		chooser.setPosition(width / 2f - chooser.getWidth() / 2, height / 2f - chooser.getHeight() / 2);

		// Square, in the margin left of the chooser, never narrower than its word.
		float buttonSize = Math.max(chooser.getX() * 0.75f, createNew.getPrefWidth());
		createNew.setBounds((chooser.getX() - buttonSize) / 2, height / 2f - buttonSize / 2f, buttonSize, buttonSize);

		title.setSize(chooser.getWidth(), (height - chooser.getHeight()) / 2);
		title.setPosition(chooser.getX(), chooser.getY() + chooser.getHeight());
	}

	private static FileFilter buildFileFilter()
	{
		return file ->
		{
			if (!file.isFile())
				return true;

			String extension = Utils_Scene2D.getExtension(file);
			return PARALLAX.equals(extension) || ATLAS.equals(extension) || PARALLAX_PROJECT.equals(extension) || JSON_PARALLAX.equals(extension);
		};
	}

	/** Opens the edition view for a project, parallax or atlas file. Returns false if it is none of those or unreadable. */
	public static boolean selectSingleFile(FileHandle file)
	{
		String extension = file.extension();
		if (!PARALLAX.equals(extension) && !JSON_PARALLAX.equals(extension) && !ATLAS.equals(extension) && !PARALLAX_PROJECT.equals(extension))
			return false;

		Object toOpen;
		try
		{
			if (PARALLAX.equals(extension))
				toOpen = Utils_Page.loadPage(file);
			else if (JSON_PARALLAX.equals(extension))
				toOpen = GVars_Serialization_Editor.objectMapper.readValue(file.file(), WholePage_Model.class);
			else if (ATLAS.equals(extension))
				toOpen = new TextureAtlas(file);
			else
				toOpen = GVars_Serialization_Editor.objectMapper.readValue(file.file(), Project_Data.class);
		}
		catch (Exception e)
		{
			Gdx.app.error("Vue_Selection", "Cannot open " + file, e);
			if (GVars_UI.mainUi != null)
				Dialogs.showErrorDialog(GVars_UI.mainUi, "Cannot open " + file.name(), e);
			return false;
		}

		projectInfos = new Project_Infos();
		projectInfos.setPathInfo(file);
		projectDatas = toOpen instanceof Project_Data ? (Project_Data) toOpen : new Project_Data();
		relativePath = projectInfos.projectPath;
		GVars_Heart_Editor.changeVue(new Vue_Edition(toOpen), true);
		return true;
	}

	@Override
	public void destroy()
	{GVars_UI.reset();}

	@Override
	public void update(float delta)
	{GVars_UI.mainUi.act(delta);}

	@Override
	public void render()
	{
		ScreenUtils.clear(0, 0, 0, 1);
		GVars_UI.mainUi.draw();
	}

	@Override
	public void receiveFiles(String[] files)
	{
		if (files.length == 1)
			selectSingleFile(new FileHandle(new File(files[0])));
	}

	@Override
	public void resize(int width, int height)
	{
		if (!GVars_UI.fitWindow())
		{
			layout();
			return;
		}
		// A new font: the widgets still hold the old one, disposed. Built again, in the folder the chooser was in.
		FileHandle directory = chooser.getDirectory();
		GVars_UI.mainUi.clear();
		init();
		chooser.setDirectory(directory);
	}
}
