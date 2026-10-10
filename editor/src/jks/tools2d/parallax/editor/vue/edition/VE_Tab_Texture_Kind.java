package jks.tools2d.parallax.editor.vue.edition;

import static jks.tools2d.parallax.editor.gvars.GVars_UI.baseSkin;
import static jks.tools2d.parallax.editor.gvars.GVars_Vue_Edition.currentlySelectedParallax;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.Tooltip;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisWindow;

import jks.tools2d.filechooser.FC_List;
import jks.tools2d.filechooser.FileChooser_Listener;
import jks.tools2d.libgdxutils.JksForm;
import jks.tools2d.libgdxutils.JksNumberSlider;
import jks.tools2d.libgdxutils.Utils_Interface;
import jks.tools2d.parallax.EffectSupport.Engine;
import jks.tools2d.parallax.ParallaxLayer;
import jks.tools2d.parallax.editor.gvars.GVars_UI;
import jks.tools2d.parallax.editor.gvars.GVars_Vue_Edition;
import jks.tools2d.parallax.editor.vue.edition.utils.Utils_LayerKind;
import jks.tools2d.parallax.editor.vue.edition.utils.Utils_LayerKind.Mark;
import jks.tools2d.parallax.pages.Enum_LayerKind;
import jks.tools2d.parallax.pages.Enum_ParticleAnchor;
import jks.tools2d.parallax.pages.Enum_ShaderEffect;

/**
 * The selected layer's kind and what only that kind has: EMPTY its name, PARTICLES its effect files and anchor, SHADER
 * its effect and numbers, SEQUENCE its segments ({@link VE_Tab_Texture_Sequence}). Above them, a mark per engine says whether it draws the layer (docs/effect-layers.md, "Saying
 * what an engine cannot draw"); a control only some engines read says which.
 */
public class VE_Tab_Texture_Kind extends JksForm
{
	private static final Enum_LayerKind[] EDITABLE_KINDS = { Enum_LayerKind.IMAGE, Enum_LayerKind.EMPTY, Enum_LayerKind.PARTICLES, Enum_LayerKind.SHADER, Enum_LayerKind.SEQUENCE };
	private static final Color DRAWN = new Color(0.45f, 0.85f, 0.45f, 1), MISSING = new Color(1, 0.7f, 0.25f, 1), NOT_DRAWN = new Color(1, 0.4f, 0.4f, 1);

	private final SelectBox<Enum_LayerKind> kind = new SelectBox<>(baseSkin);
	private final Map<Engine, VisLabel> engineMarks = new EnumMap<>(Engine.class);
	private final Map<Engine, Tooltip> engineWhy = new EnumMap<>(Engine.class);

	private final TextField name = new TextField("", baseSkin);
	private final VisLabel nameTitle = new VisLabel();

	private final TextField particlesLibgdx = new TextField("", baseSkin), particlesGodot = new TextField("", baseSkin);
	private final VisLabel particlesLibgdxStatus = new VisLabel(), particlesGodotStatus = new VisLabel();
	private final TextButton particlesLibgdxPick = new TextButton("...", baseSkin), particlesGodotPick = new TextButton("...", baseSkin);
	private final SelectBox<Enum_ParticleAnchor> particlesAnchor = new SelectBox<>(baseSkin);

	private final SelectBox<Enum_ShaderEffect> shaderEffect = new SelectBox<>(baseSkin);
	private final JksNumberSlider shaderAmplitude = shaderSlider(0, 2, 0.01f, ParallaxLayer::setShaderAmplitude);
	private final JksNumberSlider shaderWavelength = shaderSlider(0, 10, 0.05f, ParallaxLayer::setShaderWavelength);
	private final JksNumberSlider shaderSpeed = shaderSlider(-5, 5, 0.05f, ParallaxLayer::setShaderSpeed);

	private final Table engines = new Table();
	private final VisLabel particlesLibgdxTitle = titled("libGDX .p", "A libGDX particle effect: libGDX, browser"),
			particlesGodotTitle = titled("Godot .tscn", "A Godot scene: Godot"), anchorTitle = titled("Anchor", "libGDX, browser, Godot");
	private final VE_Tab_Texture_Sequence sequenceRows;

	/** Called with the rebuilt layer after the kind changed: the tab refreshes everything else from it. */
	private final Runnable onKindChanged;
	private boolean updating;

	public VE_Tab_Texture_Kind(float nameWidth, Runnable onKindChanged)
	{
		super(nameWidth);
		this.onKindChanged = onKindChanged;
		sequenceRows = new VE_Tab_Texture_Sequence(nameWidth, onKindChanged);

		kind.setName("texture.kind");
		name.setName("texture.name");
		particlesLibgdx.setName("texture.particlesLibgdx");
		particlesLibgdxStatus.setName("texture.particlesLibgdx.status");
		particlesLibgdxPick.setName("texture.particlesLibgdx.pick");
		particlesGodot.setName("texture.particlesGodot");
		particlesGodotStatus.setName("texture.particlesGodot.status");
		particlesGodotPick.setName("texture.particlesGodot.pick");
		particlesAnchor.setName("texture.particlesAnchor");
		shaderEffect.setName("texture.shaderEffect");
		shaderAmplitude.setName("texture.shaderAmplitude");
		shaderWavelength.setName("texture.shaderWavelength");
		shaderSpeed.setName("texture.shaderSpeed");

		kind.setItems(EDITABLE_KINDS);
		particlesAnchor.setItems(Enum_ParticleAnchor.values());
		shaderEffect.setItems(Enum_ShaderEffect.values());

		for (Engine engine : Engine.values())
		{
			VisLabel mark = new VisLabel(engineTitle(engine));
			mark.setName("texture.engine." + engine.name().toLowerCase());
			engineMarks.put(engine, mark);
			engineWhy.put(engine, new Tooltip.Builder("").target(mark).build());
			// Two a row: four in one are wider than the panel once they say "missing".
			engines.add(mark).pad(0, 4, 0, 4);
			if (engine == Engine.BROWSER)
				engines.row();
		}

		onChange(kind, this::changeKind);
		onChange(name, () ->
		{
			currentlySelectedParallax.setName(name.getText().isEmpty() ? null : name.getText());
			refreshMarks();
		});
		onChange(particlesLibgdx, () -> setLibgdxEffect(particlesLibgdx.getText()));
		onChange(particlesGodot, () -> setGodotScene(particlesGodot.getText()));
		onChange(particlesAnchor, () -> currentlySelectedParallax.setAnchor(particlesAnchor.getSelected()));
		onChange(shaderEffect, () ->
		{
			Utils_LayerKind.setShaderEffect(currentlySelectedParallax, shaderEffect.getSelected());
			updating = true;
			showShaderNumbers(currentlySelectedParallax);
			updating = false;
			refreshMarks();
		});
		particlesLibgdxPick.addListener(Utils_Interface.changeListener(() -> pick("libGDX effect", "p", particlesLibgdx)));
		particlesGodotPick.addListener(Utils_Interface.changeListener(() -> pick("Godot scene", "tscn", particlesGodot)));

	}

	/** A name whose tooltip says what it is and which engines read it. */
	private static VisLabel titled(String title, String why)
	{
		VisLabel label = new VisLabel(title);
		new Tooltip.Builder(why).target(label).build();
		return label;
	}

	private static String engineTitle(Engine engine)
	{
		switch (engine)
		{
			case LIBGDX:
				return "libGDX";
			case BROWSER:
				return "browser";
			case GODOT:
				return "Godot";
			default:
				return "jME";
		}
	}

	private JksNumberSlider shaderSlider(float min, float max, float step, java.util.function.BiConsumer<ParallaxLayer, Float> setter)
	{
		return new JksNumberSlider(min, max, step, baseSkin)
		{
			@Override
			public void actionOnSliderMovement()
			{
				if (!updating && currentlySelectedParallax != null)
					setter.accept(currentlySelectedParallax, getValue());
			}
		};
	}

	private void onChange(com.badlogic.gdx.scenes.scene2d.Actor actor, Runnable action)
	{
		actor.addListener(Utils_Interface.changeListener(() ->
		{
			if (!updating && currentlySelectedParallax != null)
				action.run();
		}));
	}

	private void changeKind()
	{
		ParallaxLayer layer = currentlySelectedParallax;
		ParallaxLayer rebuilt = layer.kind == kind.getSelected() ? null : Utils_LayerKind.withKind(layer, kind.getSelected());
		if (rebuilt == null)
		{
			update();
			return;
		}

		GVars_Vue_Edition.replaceLayer(layer, rebuilt);
		onKindChanged.run();
	}

	private void setLibgdxEffect(String path)
	{
		currentlySelectedParallax.setParticlesLibgdx(path.isEmpty() ? null : path);
		showLibgdxStatus(Utils_LayerKind.loadLibgdxEffect(currentlySelectedParallax));
		refreshMarks();
	}

	private void setGodotScene(String path)
	{
		currentlySelectedParallax.setParticlesGodot(path.isEmpty() ? null : path);
		showGodotStatus();
		refreshMarks();
	}

	private void showLibgdxStatus(String whyNotPlaying)
	{
		String path = currentlySelectedParallax.getParticlesLibgdx();
		if (path == null)
			status(particlesLibgdxStatus, "none: libGDX draws nothing", MISSING);
		else if (whyNotPlaying != null)
			status(particlesLibgdxStatus, "not played: " + whyNotPlaying, NOT_DRAWN);
		else
			status(particlesLibgdxStatus, "playing in the preview", DRAWN);
	}

	private void showGodotStatus()
	{
		String path = currentlySelectedParallax.getParticlesGodot();
		if (path == null)
			status(particlesGodotStatus, "none: Godot draws nothing", MISSING);
		else if (!Utils_LayerKind.exists(path))
			status(particlesGodotStatus, "not found beside the atlas", NOT_DRAWN);
		else
			status(particlesGodotStatus, "found (Godot plays it, not the preview)", DRAWN);
	}

	private static void status(Label label, String text, Color color)
	{
		label.setText(text);
		label.setColor(color);
	}

	/** A file chooser in a window, from the atlas's folder: the picked file goes in {@code field}, relative to it. */
	private void pick(String what, String extension, TextField field)
	{
		VisWindow window = new VisWindow("Pick the " + what + " (." + extension + ")");
		window.setName("texture.picker");
		FC_List chooser = new FC_List(baseSkin, new FileChooser_Listener()
		{
			@Override
			public void choose(FileHandle file)
			{
				window.remove();
				field.setText(Utils_LayerKind.pagePath(file.file().toPath()));
				field.fire(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent());
			}

			@Override
			public void choose(Array<FileHandle> files)
			{}

			@Override
			public void cancel()
			{window.remove();}
		});
		chooser.setFileFilter(file -> file.isDirectory() || file.getName().endsWith("." + extension));
		File folder = Utils_LayerKind.effectFolder() == null ? null : Utils_LayerKind.effectFolder().toFile();
		if (folder != null)
			chooser.setStartDirectory(new FileHandle(folder));
		chooser.setName("texture.picker.chooser");

		window.add(chooser).width(GVars_UI.mainUi.getWidth() * 0.6f).height(GVars_UI.mainUi.getHeight() * 0.6f);
		window.pack();
		window.centerWindow();
		GVars_UI.mainUi.addActor(window);
	}

	/** Refreshes every control from the selected layer, and shows the rows of its kind. */
	public void update()
	{
		ParallaxLayer layer = currentlySelectedParallax;
		updating = true;

		kind.setSelected(layer.kind);

		name.setText(layer.getName() == null ? "" : layer.getName());
		nameTitle.setText(layer.kind == Enum_LayerKind.EMPTY ? "Hook key" : "Name");
		particlesLibgdx.setText(layer.getParticlesLibgdx() == null ? "" : layer.getParticlesLibgdx());
		particlesGodot.setText(layer.getParticlesGodot() == null ? "" : layer.getParticlesGodot());
		particlesAnchor.setSelected(layer.getAnchor());
		shaderEffect.setSelected(layer.getShaderEffect());
		showShaderNumbers(layer);

		// One line a setting, the kind's own under the rest (r67); the depth fog is the page's (Background, Fog).
		clearChildren();
		line("Kind", kind);
		wide(engines);
		line(nameTitle, name);
		if (layer.kind == Enum_LayerKind.PARTICLES)
		{
			line(particlesLibgdxTitle, particlesLibgdx, particlesLibgdxPick);
			under(particlesLibgdxStatus);
			line(particlesGodotTitle, particlesGodot, particlesGodotPick);
			under(particlesGodotStatus);
			line(anchorTitle, particlesAnchor);
			showLibgdxStatus(layer.getParticlesLibgdx() != null && layer.getParticles() == null ? "not loaded" : null);
			showGodotStatus();
		}
		if (layer.kind == Enum_LayerKind.SHADER)
		{
			line("Effect", shaderEffect);
			line("Amplitude", shaderAmplitude);
			line("Wavelength", shaderWavelength);
			line("Speed", shaderSpeed);
		}
		if (layer.kind == Enum_LayerKind.SEQUENCE)
		{
			wide(sequenceRows);
			sequenceRows.update();
		}

		refreshMarks();
		updating = false;
	}

	private void showShaderNumbers(ParallaxLayer layer)
	{
		shaderAmplitude.setValue(layer.getShaderAmplitude());
		shaderWavelength.setValue(layer.getShaderWavelength());
		shaderSpeed.setValue(layer.getShaderSpeed());
	}

	/** Colors each engine's mark: green it draws the layer, orange it lacks something, red it cannot draw it. */
	private void refreshMarks()
	{
		ParallaxLayer layer = currentlySelectedParallax;
		for (Engine engine : Engine.values())
		{
			Mark mark = Utils_LayerKind.mark(layer, engine);
			String why = Utils_LayerKind.why(layer, engine);
			VisLabel label = engineMarks.get(engine);
			label.setText(engineTitle(engine) + (mark == Mark.DRAWN ? " ok" : mark == Mark.MISSING ? " missing" : " no"));
			label.setColor(mark == Mark.DRAWN ? DRAWN : mark == Mark.MISSING ? MISSING : NOT_DRAWN);
			engineWhy.get(engine).setText(why == null ? engineTitle(engine) + " draws this layer" : why);
		}
	}
}
