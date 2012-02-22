package world;

import java.util.Calendar;
import peasy.PeasyCam;
import processing.core.PApplet;
import controlP5.ControlP5;
import controlP5.ControlWindow;
import controlP5.Slider;
import ddf.minim.AudioInput;
import ddf.minim.AudioOutput;
import ddf.minim.AudioPlayer;
import ddf.minim.Minim;
import ddf.minim.analysis.BeatDetect;
import ddf.minim.analysis.FFT;
import basics.SaveFrameHandler;

/*
 * TODO: 
 * 1. kamera soll sich von allein immer wieder nah an das objekt und wieder wegbewegen (5sekunden für einen weg)
 * 2. GSVideo export anschauen. falscher codec (mit vimeo codec vergleichen)
 * 3. per drawGraph(PApplet p) den track untersuchen und  dann per isRange() speziell für den cylob track die audioreaktion anpassen
 * 4. den scheiss code unten kommentieren
 * 5. vielleicht ein geileres objekt, dass sich an den track angepasst verändert (toxiclibs, isosurface)
 * 6. code besser strukturieren
 * 
 */

public class AudioreactiveProject extends PApplet {
	private static final long serialVersionUID = -3174601024298984954L;

	
	PeasyCam cam;
	
	//for moviemaker
	int fps = 30;

	Minim minim;
	AudioPlayer jingle;
	FFT fft;
	BeatDetect beat;
	BeatListener bl;

	//mrzlib
	SaveFrameHandler save;
	
	ControlP5 controlP5;
	ControlWindow controlWindow;

	//beatsensitivityslider
	Slider beatsens;
	
	//slider for max and min val for the angle of the connecting lines (reacts on kick)
	Slider kickSpinAngleSliderMin;
	Slider kickSpinAngleSliderMax;
	//slider for the multiplier which is multiplied by the angle every frame
	Slider kickSpinAngleMultiplierSlider;
	
	//slider for max and min val of the radius of the circles (reacts on kick)
	Slider kickRadiusSliderMin;
	Slider kickRadiusSliderMax;
	//slider for the multiplier which is multiplied by the radius every frame
	Slider kickRadiusMultiplierSlider;
	
	//slider for min and max values of the number of the lines connecting the two circles (reacts on kick)
	Slider kickLineCountSliderMin;
	Slider kickLineCountSliderMax;
	//slider for multiplier which is multiplied by the number of lines every frame
	Slider kickLineCountMultiplierSlider;
	
	//slider for the min and max red stroke value (kick)
	Slider redStrokeColorSliderMin;
	Slider redStrokeColorSliderMax;
	Slider redStrokeColorMultiplierSlider;

	
	int beatSensitivity;
	
	//starting values of all parameters which are changed according to the beats
	float kickSpinAngle = radians(20);
	float kickRadius = 100;
	float kickLineCount = 300;
	float redStrokeColor = 0;

	//values which are changed by the sliders
	float kickSpinAngleMin;
	float kickSpinAngleMax;
	float kickSpinAngleMultiplier;
	float kickRadiusMin;
	float kickRadiusMax;
	float kickRadiusMultiplier;
	float kickLineCountMin;
	float kickLineCountMax;
	float kickLineCountMultiplier;
	float redStrokeColorMin;
	float redStrokeColorMax;
	float redStrokeColorMultiplier;
	float hihatBackground;

	/*
	 * @see processing.core.PApplet#setup()
	 */
	public void setup() {
		size(1280, 720, OPENGL);
		//aa
		hint(ENABLE_OPENGL_4X_SMOOTH);
		frameRate(fps);

		//init framesaver
		save = new SaveFrameHandler(this);

		//init of min and max values
		kickSpinAngleMin = radians(20);
		kickSpinAngleMax = radians(170);
		kickSpinAngleMultiplier = 0.9f;
		kickRadiusMin = 100;
		kickRadiusMax = 800;
		kickRadiusMultiplier = 0.9f;
		kickLineCountMin = 20;
		kickLineCountMax = 300;
		kickLineCountMultiplier = 0.999f;

		hihatBackground = 0;

		// ----------------------------------------------------------------------- CONTROLP5
		controlP5 = new ControlP5(this);
		controlWindow = controlP5.addControlWindow("controlWindow", 1500, 100,
				900, 600);
		controlWindow.hideCoordinates();
		controlWindow.setTitle("control");

		beatsens = controlP5.addSlider("beatSensitivity", 10, 500, 100, 10, 30,
				790, 20);
		beatsens.moveTo(controlWindow);

		kickSpinAngleSliderMin = controlP5.addSlider("kickSpinAngleMin",
				radians(0), radians(180), radians(20), 10, 60, 200, 20);
		kickSpinAngleSliderMin.moveTo(controlWindow);
		kickSpinAngleSliderMax = controlP5.addSlider("kickSpinAngleMax",
				radians(0), radians(180), radians(170), 300, 60, 200, 20);
		kickSpinAngleSliderMax.moveTo(controlWindow);
		kickSpinAngleMultiplierSlider = controlP5.addSlider(
				"kickSpinAngleMultiplier", 0.8f, 1, 0.9f, 600, 60, 200, 20);
		kickSpinAngleMultiplierSlider.moveTo(controlWindow);

		kickRadiusSliderMin = controlP5.addSlider("kickRadiusMin", 20, 1000,
				100, 10, 90, 200, 20);
		kickRadiusSliderMin.moveTo(controlWindow);
		kickRadiusSliderMax = controlP5.addSlider("kickRadiusMax", 20, 1000,
				800, 300, 90, 200, 20);
		kickRadiusSliderMax.moveTo(controlWindow);
		kickRadiusMultiplierSlider = controlP5.addSlider(
				"kickRadiusMultiplier", 0.8f, 1, 0.9f, 600, 90, 200, 20);
		kickRadiusMultiplierSlider.moveTo(controlWindow);

		kickLineCountSliderMin = controlP5.addSlider("kickLineCountMin", 0,
				500, 20, 10, 120, 200, 20);
		kickLineCountSliderMin.moveTo(controlWindow);
		kickLineCountSliderMax = controlP5.addSlider("kickLineCountMax", 0,
				500, 300, 300, 120, 200, 20);
		kickLineCountSliderMax.moveTo(controlWindow);
		kickLineCountMultiplierSlider = controlP5.addSlider(
				"kickLineCountMultiplier", 0.8f, 1, 0.999f, 600, 120, 200, 20);
		kickLineCountMultiplierSlider.moveTo(controlWindow);

		// -------------------------------------------------------------------------- PEASYCAM
		cam = new PeasyCam(this, 1400);
		cam.setMinimumDistance(100);
		cam.setMaximumDistance(7000);

		// -------------------------------------------------------------------------- MINIM
		minim = new Minim(this);
		jingle = minim
				.loadFile(
						"cylob.mp3",
						4096);
		jingle.play();
		//minim.debugOn();
		//jingle = minim.(Minim.STEREO, 2048);
		beat = new BeatDetect(jingle.bufferSize(), jingle.sampleRate());
		// set the sensitivity to 100 milliseconds
		// After a beat has been detected, the algorithm will wait for 100
		// milliseconds
		// before allowing another beat to be reported. You can use this to
		// dampen the
		// algorithm if it is giving too many false-positives. The default value
		// is 10,

		beatSensitivity = 100;
		beat.setSensitivity(beatSensitivity);
		bl = new BeatListener(beat, jingle);
	}

	/*
	 * @see processing.core.PApplet#draw()
	 */
	public void draw() {
		background(hihatBackground);
		stroke(redStrokeColor, 0, 0, 250);
		strokeWeight(0.5f);

		cam.rotateZ(0.05);

		drawShape();
		loadPixels();
	}

	private void drawShape() {
		//resets the sensitivity of the beat to the values thats set w/ the slider
		beat.setSensitivity(beatSensitivity);
		//detect beat
		beat.detect(jingle.mix);
		//selfexplanatory
		if (beat.isKick()) {
			kickRadius = kickRadiusMax;
			kickSpinAngle = kickSpinAngleMax;
			redStrokeColor = 255;
			kickLineCount = kickLineCountMax;

		} else if (beat.isHat()) {
			hihatBackground = 100;
		} else if (beat.isSnare()) {

		}
		
		//calculating the circles and connections
		float angle = TWO_PI / (float) kickLineCount;
		for (int i = 0; i < kickLineCount; i++) {
			point(kickRadius * sin(angle * i), 200, kickRadius * cos(angle * i));
			point(kickRadius * sin(angle * i), -200, kickRadius
					* cos(angle * i));

			line(kickRadius * sin(angle * i), 200, kickRadius * cos(angle * i),
					kickRadius * sin(angle * i + kickSpinAngle), -200,
					kickRadius * cos(angle * i + kickSpinAngle));
		}

		//here's where the min and max values come into the game
		kickSpinAngle = constrain(kickSpinAngle * kickSpinAngleMultiplier,
				kickSpinAngleMin, kickSpinAngleMax);
		kickRadius = constrain(kickRadius * kickRadiusMultiplier,
				kickRadiusMin, kickRadiusMax);
		kickLineCount = constrain(kickLineCount * kickLineCountMultiplier,
				kickLineCountMin, kickLineCountMax);
		redStrokeColor = constrain(redStrokeColor * 0.95f, 0, 240);
		hihatBackground = constrain(redStrokeColor * 1.05f, 0, 240);
	}

	/*
	 * @see processing.core.PApplet#stop()
	 */
	public void stop() {
		// always close Minim audio classes when you are finished with them
		jingle.close();
		// always stop Minim before exiting
		minim.stop();
		super.stop();
	}

	/*
	 * @see processing.core.PApplet#keyPressed()
	 */
	public void keyPressed() {
		if (key == ' ') {
			exit();
		} else if (key == 's') {
			//saveframe
			save.save();
		}
	}

	/*
	 * 
	 */
	public static void main(String args[]) {
		PApplet.main(new String[] { "--present", "AudioreactiveProject" });
	}

}
