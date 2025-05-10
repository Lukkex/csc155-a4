package a4;

import java.nio.*;
import java.util.ArrayList;

import javax.swing.*;
import java.lang.Math;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_BACK;
import static com.jogamp.opengl.GL.GL_BLEND;
import static com.jogamp.opengl.GL.GL_CCW;
import static com.jogamp.opengl.GL.GL_CLAMP_TO_EDGE;
import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_CULL_FACE;
import static com.jogamp.opengl.GL.GL_DEPTH_ATTACHMENT;
import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_COMPONENT32;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_ELEMENT_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_FRAMEBUFFER;
import static com.jogamp.opengl.GL.GL_FRONT;
import static com.jogamp.opengl.GL.GL_FRONT_AND_BACK;
import static com.jogamp.opengl.GL.GL_FUNC_ADD;
import static com.jogamp.opengl.GL.GL_LEQUAL;
import static com.jogamp.opengl.GL.GL_LINEAR;
import static com.jogamp.opengl.GL.GL_LINES;
import static com.jogamp.opengl.GL.GL_NONE;
import static com.jogamp.opengl.GL.GL_ONE_MINUS_SRC_ALPHA;
import static com.jogamp.opengl.GL.GL_POINTS;
import static com.jogamp.opengl.GL.GL_POLYGON_OFFSET_FILL;
import static com.jogamp.opengl.GL.GL_REPEAT;
import static com.jogamp.opengl.GL.GL_SRC_ALPHA;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TEXTURE0;
import static com.jogamp.opengl.GL.GL_TEXTURE1;
import static com.jogamp.opengl.GL.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL.GL_TEXTURE_CUBE_MAP;
import static com.jogamp.opengl.GL.GL_TEXTURE_MAG_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_MIN_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_S;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_T;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL.GL_UNSIGNED_INT;
import static com.jogamp.opengl.GL2ES2.GL_COMPARE_REF_TO_TEXTURE;
import static com.jogamp.opengl.GL2ES2.GL_DEPTH_COMPONENT;
import static com.jogamp.opengl.GL2ES2.GL_TEXTURE_COMPARE_FUNC;
import static com.jogamp.opengl.GL2ES2.GL_TEXTURE_COMPARE_MODE;
import static com.jogamp.opengl.GL2GL3.GL_FILL;
import static com.jogamp.opengl.GL2GL3.GL_TEXTURE_CUBE_MAP_SEAMLESS;
import static com.jogamp.opengl.GL3ES3.GL_PATCHES;
import static com.jogamp.opengl.GL3ES3.GL_PATCH_VERTICES;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.*;
import com.jogamp.common.nio.Buffers;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

import org.joml.*;

public class Code extends JFrame implements GLEventListener, KeyListener
{	private GLCanvas myCanvas;
	private int renderingProgram1, renderingProgram2, renderingProgramGeom, renderingProgramTess, renderingProgramCubeMap;
	private final int numOfModels = 9;
	private final int numOfObjects = numOfModels + 5;
	private int numOfBuffersPerObject = 3;
	private ImportedModel models[] = new ImportedModel[numOfModels];
	private int textures[] = new int[numOfObjects];
	private Platform plat = new Platform();
	private boolean areAxesVisible = true;
	private int vao[] = new int[1];
	private int vbo[] = new int[numOfObjects*numOfBuffersPerObject];

	//Skybox
	private int skyboxVBO[] = new int[1];
	private int skyboxTexture;
	private Matrix4f skyboxModelMat = new Matrix4f();
	
	//Matrix Stacks
	private Matrix4fStack mvStack = new Matrix4fStack(5);

	//Used for position, texture coordinates, and normal vector values and plugging them into buffers
	private ArrayList<float[]> allpvalues = new ArrayList<float[]>();
	private ArrayList<float[]> alltvalues = new ArrayList<float[]>();
	private ArrayList<float[]> allnvalues = new ArrayList<float[]>();
	private float[] pvalues, tvalues, nvalues;
	private Vector3f[] vertices;
	private Vector2f[] texCoords;
	private Vector3f[] normals;
	private FloatBuffer vertBuf, texBuf, norBuf;
	private int numObjVertices = 0;

	//World Vector3f components
	Vector3f worldRightVector = new Vector3f(1f, 0f, 0f);
	Vector3f worldUpVector = new Vector3f(0f, 1f, 0f);
	Vector3f worldForwardVector = new Vector3f(0f, 0f, -1f);
	Vector3f worldOrigin = new Vector3f(0f, 0f, 0f);

	//Camera components
	private Camera cam = new Camera(this);
	private final float DEFAULT_CAM_X = 0.0f;
	private final float DEFAULT_CAM_Y = 0.0f;
	private final float DEFAULT_CAM_Z = 4.5f;

	private Matrix4f[] invTrsMatrices = new Matrix4f[numOfObjects]; //inverted transpose matrix
	private Matrix4f[] modelMatrices = new Matrix4f[numOfObjects]; //Stores all the model matrices for all models
	private Matrix4f temp = new Matrix4f();
	private int mvLoc;
	private double rotateInc = 0.0;
	private float deltaTime = 0.0f;
	private float pitchAmount = 0.02f;
	private float panAmount = 0.02f;
	private float movementSpeed = 0.02f;
	private float rotationSpeed = 0.02f;
	private double floatingState = 0.0;
	private long lastTime = 0L;
	private long curTime = 0L;
	private float axisLineLength = 50f; //Made to be large enough so you can see it outside the inner chamber

	//Lights
	private float amt = 0.0f;
	private Vector3f cameraLoc = new Vector3f(DEFAULT_CAM_X, DEFAULT_CAM_Y, DEFAULT_CAM_Z);
	private Vector3f lightLoc = new Vector3f(-9.8f, 2.2f, 1.1f);
	
	//White light properties
	float[] globalAmbient = new float[] { 0.8f, 0.8f, 0.8f, 1.0f };
	float[] lightAmbient = new float[] { 0.1f, 0.1f, 0.1f, 1.0f };
	float[] lightDiffuse = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
	float[] lightSpecular = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
		
	//Materials
	private final int DEFAULT_MATERIAL = 0;
	private int material = DEFAULT_MATERIAL;

	// material 1
	private float[] GmatAmb = {0.3f, 0.3f, 0.3f};
	private float[] GmatDif = {0.4f, 0.4f, 0.4f};
	private float[] GmatSpe = {0.4f, 0.4f, 0.4f};
	private float GmatShi = Utils.goldShininess();
	
	// material 2
	private float[] BmatAmb = {0.2f, 0.2f, 0.2f};
	private float[] BmatDif = {0.2f, 0.2f, 0.2f};
	private float[] BmatSpe = {0.2f, 0.2f, 0.2f};
	private float BmatShi = Utils.bronzeShininess();
	
	private float[] thisAmb, thisDif, thisSpe, matAmb, matDif, matSpe;
	private float thisShi, matShi;
	
	// shadow stuff
	private int scSizeX, scSizeY;
	private int [] shadowTex = new int[1];
	private int [] shadowBuffer = new int[1];
	private Matrix4f lightVmat = new Matrix4f();
	private Matrix4f lightPmat = new Matrix4f();
	private Matrix4f shadowMVP1 = new Matrix4f();
	private Matrix4f shadowMVP2 = new Matrix4f();
	private Matrix4f b = new Matrix4f();

	//Tessellation
	private Vector3f terLoc = new Vector3f(0.0f, -1.0f, -3.3f);
	private int floorTexture;
	private float tessInner = 30.0f;
	private float tessOuter = 20.0f;

	// allocate variables for display() function
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);
	private Matrix4f pMat = new Matrix4f();  // perspective matrix
	private Matrix4f vMat = new Matrix4f();  // view matrix
	private Matrix4f mMat = new Matrix4f();  // model matrix
	private Matrix4f mvMat = new Matrix4f(); // model-view matrix
	private Matrix4f mvpMat = new Matrix4f();
	private Matrix4f invTrMat = new Matrix4f(); // inverse-transpose
	private int mLoc, vLoc, pLoc, nLoc, sLoc, alphaLoc, flipLoc, mvpLoc;
	private boolean transparent;
	private int globalAmbLoc, ambLoc, diffLoc, specLoc, posLoc, mambLoc, mdiffLoc, mspecLoc, mshiLoc;
	private float aspect;
	private Vector3f currentLightPos = new Vector3f();
	private float[] lightPos = new float[3];
	private Vector3f origin = new Vector3f(0.0f, 0.0f, 0.0f);
	private Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
	
	public Code()
	{	setTitle("CSC155 - Lab #4");
		setSize(1000, 1000);
		myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);
		myCanvas.addKeyListener(this);
		this.add(myCanvas);
		this.setVisible(true);
		Animator animator = new Animator(myCanvas);
		animator.start();
	}

	public void init(GLAutoDrawable drawable){	
		GL4 gl = (GL4) GLContext.getCurrentGL();

		//Sets all mMats for all models to a default identity matrix
		for (int i = 0; i < numOfObjects; i++){
			modelMatrices[i] = new Matrix4f().identity();
			invTrsMatrices[i] = new Matrix4f().identity();
		}

		models[0] = new ImportedModel("models/GHOUL.obj");
		textures[0] = Utils.loadTexture("textures/GHOUL.jpg");

		models[1] = new ImportedModel("models/Sanctum.obj");
		textures[1] = Utils.loadTexture("textures/Sanctum.jpg");

		models[2] = new ImportedModel("models/Star.obj");
		textures[2] = Utils.loadTexture("textures/Star.jpg");

		models[3] = new ImportedModel("models/Chamber.obj");
		textures[3] = Utils.loadTexture("textures/Chamber.jpg");

		models[4] = new ImportedModel("models/UpperChamber.obj");
		textures[4] = Utils.loadTexture("textures/Star.jpg");

		models[5] = new ImportedModel("models/crazyeye.obj");
		textures[5] = Utils.loadTexture("textures/crazyeye.png");
		modelMatrices[5].rotateX((float) Math.toRadians(90.0));
		modelMatrices[5].scale(new Vector3f(0.25f, 0.25f, 0.25f));

		models[6] = new ImportedModel("models/outerStars1.obj");
		textures[6] = Utils.loadTexture("textures/Star.jpg");
		modelMatrices[6].scale(new Vector3f(0.90f, 0.90f, 0.90f));

		models[7] = new ImportedModel("models/outerStars2.obj");
		textures[7] = Utils.loadTexture("textures/Star.jpg");

		models[8] = new ImportedModel("models/Cone.obj");
		textures[8] = Utils.loadTexture("textures/brick1.jpg"); //From the book
		modelMatrices[8].translate(new Vector3f(0f, -0.25f, 0f)); // Starts off lower in the world

		//Objects without any external models
		textures[numOfModels] = Utils.loadTexture("textures/eyefloor.png"); //Custom
		modelMatrices[numOfModels].translate(new Vector3f(0f, -3f, 0f)); // Starts off lower in the world

		textures[numOfModels+1] = Utils.loadTexture("textures/X.png");

		textures[numOfModels+2] = Utils.loadTexture("textures/Y.png");

		textures[numOfModels+3] = Utils.loadTexture("textures/Z.png");

		textures[numOfModels+4] = Utils.loadTexture("textures/sun.png");

		renderingProgram1 = Utils.createShaderProgram("a4/vert1shader.glsl", "a4/frag1shader.glsl");
		renderingProgram2 = Utils.createShaderProgram("a4/vert2shader.glsl", "a4/frag2shader.glsl");
		renderingProgramGeom = Utils.createShaderProgram("a4/vertGeomShader.glsl", "a4/geomShader.glsl", "a4/fragGeomShader.glsl");
		renderingProgramTess = Utils.createShaderProgram("a4/vertShader.glsl", "a4/tessCShader.glsl", "a4/tessEShader.glsl", "a4/fragShader.glsl");
		renderingProgramCubeMap = Utils.createShaderProgram("a4/vertCShader.glsl", "a4/fragCShader.glsl");

		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

		cam.setX(cameraLoc.x());
		cam.setY(cameraLoc.y());
		cam.setZ(cameraLoc.z());

		setupVertices();
		setupShadowBuffers();

		//Tessellation Texture
		floorTexture = Utils.loadTexture("textures/floor_color.jpg");
				
		b.set(
			0.5f, 0.0f, 0.0f, 0.0f,
			0.0f, 0.5f, 0.0f, 0.0f,
			0.0f, 0.0f, 0.5f, 0.0f,
			0.5f, 0.5f, 0.5f, 1.0f);

		skyboxTexture = Utils.loadCubeMap("cubeMap");
		skyboxModelMat.identity();

		gl.glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);
	}

	public void display(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();

		gl.glClear(GL_COLOR_BUFFER_BIT);
		gl.glClear(GL_DEPTH_BUFFER_BIT);

		//Get view matrix for the current frame
		vMat.set(cam.buildViewMatrix());
		
		// draw cube map
		gl.glUseProgram(renderingProgramCubeMap);

		skyboxModelMat.rotateY((float)Math.toRadians(rotationSpeed * 5 * deltaTime));

		mLoc = gl.glGetUniformLocation(renderingProgram1, "m_matrix");
		gl.glUniformMatrix4fv(mLoc, 1, false, skyboxModelMat.get(vals));

		vLoc = gl.glGetUniformLocation(renderingProgramCubeMap, "v_matrix");
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));

		pLoc = gl.glGetUniformLocation(renderingProgramCubeMap, "p_matrix");
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
				
		gl.glBindBuffer(GL_ARRAY_BUFFER, skyboxVBO[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_CUBE_MAP, skyboxTexture);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);	     // cube is CW, but we are viewing the inside
		gl.glDisable(GL_DEPTH_TEST);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		tessellation(gl);
		gl.glEnable(GL_DEPTH_TEST);

		//Clears color & depth buffers to default and uses prev. created renderingProgram object
		gl.glDisable(GL_CULL_FACE);
		gl.glUseProgram(renderingProgram1);

		//Calculates delta time, used in movement functions to standardize speed across devices
		calculateDeltaTime();
		
		currentLightPos.set(lightLoc);
		currentLightPos.rotateAxis((float)Math.toRadians(amt), 0.0f, 0.0f, 1.0f);

		updateObjects(gl);
		
		lightVmat.identity().setLookAt(currentLightPos, origin, up);	// vector from light to origin
		lightPmat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

		gl.glBindFramebuffer(GL_FRAMEBUFFER, shadowBuffer[0]);
		gl.glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, shadowTex[0], 0);
	
		gl.glDrawBuffer(GL_NONE);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glEnable(GL_POLYGON_OFFSET_FILL);	//  for reducing
		gl.glPolygonOffset(3.0f, 5.0f);		//  shadow artifacts

		passOne();
		
		gl.glDisable(GL_POLYGON_OFFSET_FILL);	// artifact reduction, continued
		
		gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, shadowTex[0]);
	
		gl.glDrawBuffer(GL_FRONT);
		
		passTwo();

	}
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public void passOne()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
	
		gl.glUseProgram(renderingProgram1);

		sLoc = gl.glGetUniformLocation(renderingProgram1, "shadowMVP");

		gl.glClear(GL_DEPTH_BUFFER_BIT);

		for (int i = 0; i < numOfModels; i++){
			mMat = modelMatrices[i];

			shadowMVP1.identity();
			shadowMVP1.mul(lightPmat);
			shadowMVP1.mul(lightVmat);
			shadowMVP1.mul(mMat);

			gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));
			
			gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[i*numOfBuffersPerObject]);
			gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
			gl.glEnableVertexAttribArray(0);

			gl.glEnable(GL_CULL_FACE);
			gl.glFrontFace(GL_CCW);
			gl.glEnable(GL_DEPTH_TEST);
			gl.glDepthFunc(GL_LEQUAL);
		
			gl.glDrawArrays(GL_TRIANGLES, 0, models[i].getNumVertices());
		}
	}
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public void passTwo()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
	
		gl.glUseProgram(renderingProgram2);
		
		mLoc = gl.glGetUniformLocation(renderingProgram2, "m_matrix");
		vLoc = gl.glGetUniformLocation(renderingProgram2, "v_matrix");
		pLoc = gl.glGetUniformLocation(renderingProgram2, "p_matrix");
		nLoc = gl.glGetUniformLocation(renderingProgram2, "norm_matrix");
		sLoc = gl.glGetUniformLocation(renderingProgram2, "shadowMVP");
		alphaLoc = gl.glGetUniformLocation(renderingProgram2, "alpha");
		flipLoc = gl.glGetUniformLocation(renderingProgram2, "flipNormal");
		
		vMat.set(cam.buildViewMatrix());
		gl.glClear(GL_DEPTH_BUFFER_BIT);

		for (int i = 0; i < numOfModels; i++){
			transparent = false;
			if (i == 2 || i == 3 || i == 6 || i == 7)//Transparent Glass Chamber / Stars
				transparent = true;
			else if (i == 8){ 
				gl.glClear(GL_DEPTH_BUFFER_BIT);
				gl.glUseProgram(renderingProgramGeom);
				mLoc = gl.glGetUniformLocation(renderingProgramGeom, "m_matrix");
				vLoc = gl.glGetUniformLocation(renderingProgramGeom, "v_matrix");
				pLoc = gl.glGetUniformLocation(renderingProgramGeom, "p_matrix");
				nLoc = gl.glGetUniformLocation(renderingProgramGeom, "norm_matrix");
				sLoc = gl.glGetUniformLocation(renderingProgramGeom, "shadowMVP");
			}
			else if (i == 9) {
				gl.glClear(GL_DEPTH_BUFFER_BIT);
				gl.glUseProgram(renderingProgram2);
				mLoc = gl.glGetUniformLocation(renderingProgram2, "m_matrix");
				vLoc = gl.glGetUniformLocation(renderingProgram2, "v_matrix");
				pLoc = gl.glGetUniformLocation(renderingProgram2, "p_matrix");
				nLoc = gl.glGetUniformLocation(renderingProgram2, "norm_matrix");
				sLoc = gl.glGetUniformLocation(renderingProgram2, "shadowMVP");
			}

			if (i%2 == 0) material = 1;
			
			if (material == 0){
				thisAmb = GmatAmb;
				thisDif = GmatDif;
				thisSpe = GmatSpe;
				thisShi = GmatShi;
			}
			else if (material == 1){
				thisAmb = BmatAmb;
				thisDif = BmatDif;
				thisSpe = BmatSpe;
				thisShi = BmatShi;
			}
			
			mMat = modelMatrices[i];
			
			//Specific lighting for chamber w/ geom shader
			if (i == 8) installLights(renderingProgramGeom);
			else installLights(renderingProgram2);

			shadowMVP2.identity();
			shadowMVP2.mul(b);
			shadowMVP2.mul(lightPmat);
			shadowMVP2.mul(lightVmat);
			shadowMVP2.mul(mMat);
			
			mMat.invert(invTrMat);
			invTrMat.transpose(invTrMat);

			gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
			gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
			gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
			gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
			gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));

			if (transparent){
				gl.glProgramUniform1f(renderingProgram2, alphaLoc, 1.0f);
				gl.glProgramUniform1f(renderingProgram2, flipLoc, 1.0f);
				
				//Vertices
				gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[(i*numOfBuffersPerObject)]);
				gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
				gl.glEnableVertexAttribArray(0);

				//Normals
				gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[(i*numOfBuffersPerObject)+2]);
				gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
				gl.glEnableVertexAttribArray(1);
				
				//Texture Coordinates
				gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[(i*numOfBuffersPerObject)+1]);
				gl.glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0);
				gl.glEnableVertexAttribArray(2);
				
				gl.glActiveTexture(GL_TEXTURE1);
				gl.glBindTexture(GL_TEXTURE_2D, textures[i]);

				// 2-pass rendering a transparent version of the pyramid

				gl.glEnable(GL_BLEND);
				gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
				gl.glBlendEquation(GL_FUNC_ADD);

				gl.glEnable(GL_CULL_FACE);
				
				gl.glCullFace(GL_FRONT);
				gl.glProgramUniform1f(renderingProgram2, alphaLoc, 0.3f);
				gl.glProgramUniform1f(renderingProgram2, flipLoc, -1.0f);
				gl.glDrawArrays(GL_TRIANGLES, 0, models[i].getNumVertices());
				
				gl.glCullFace(GL_BACK);
				gl.glProgramUniform1f(renderingProgram2, alphaLoc, 0.7f);
				gl.glProgramUniform1f(renderingProgram2, flipLoc, 1.0f);
				gl.glDrawArrays(GL_TRIANGLES, 0, models[i].getNumVertices());

				gl.glDisable(GL_BLEND);
		
				transparent = true;
				// end transparency section
			}
			else {
				gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[i*numOfBuffersPerObject]);
				gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
				gl.glEnableVertexAttribArray(0);

				gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[(i*numOfBuffersPerObject)+2]);
				gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
				gl.glEnableVertexAttribArray(1); 

				gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[(i*numOfBuffersPerObject)+1]);
				gl.glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0);
				gl.glEnableVertexAttribArray(2);
				
				gl.glActiveTexture(GL_TEXTURE1);
				gl.glBindTexture(GL_TEXTURE_2D, textures[i]);

				gl.glDisable(GL_CULL_FACE);
				gl.glFrontFace(GL_CCW);
				gl.glEnable(GL_DEPTH_TEST);
				gl.glDepthFunc(GL_LEQUAL);

				gl.glDrawArrays(GL_TRIANGLES, 0, models[i].getNumVertices());
			}
		}
	}

	public void tessellation(GL4 gl){
		gl.glUseProgram(renderingProgramTess);
		
		gl.glDisable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		mvpLoc = gl.glGetUniformLocation(renderingProgramTess, "mvp");
		
		vMat.set(cam.buildViewMatrix());
		
		mMat.identity().setTranslation(terLoc.x(), terLoc.y(), terLoc.z());
		mMat.scale(7.0f, 7.0f, 7.0f);
		mMat.rotateX((float) Math.toRadians(90.0f));
		mMat.rotateY((float) Math.toRadians(1800.0f));

		mvpMat.identity();
		mvpMat.mul(pMat);
		mvpMat.mul(vMat);
		mvpMat.mul(mMat);
		
		gl.glUniformMatrix4fv(mvpLoc, 1, false, mvpMat.get(vals));
		
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, floorTexture);
	
		gl.glFrontFace(GL_CCW);

		gl.glPatchParameteri(GL_PATCH_VERTICES, 16);
		gl.glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
		gl.glDrawArrays(GL_PATCHES, 0, 16);
	}

	
	//Update all objects positions (Model Matrices)
	public void updateObjects(GL4 gl){		
		//Iterates through every single object (with a model) in the scene and updates their local positions into the world
		//using their respective model matrix and based on the view, perspective matrices as well
		for (int i = 0; i < numOfModels; i++){
			mMat = modelMatrices[i];
			invTrMat = invTrsMatrices[i];
			invTrMat.identity();

			material = DEFAULT_MATERIAL; //default is 0

			//If Ghoul, then hover up and down
			if (i == 0){ 
				material = 1; //2nd material
				temp.translation(0, (float) java.lang.Math.cos((double) ((deltaTime * floatingState)))/5000 * 10, 0);
				mMat.mul(temp); 
				floatingState += movementSpeed/10;
				floatingState %= (2 * java.lang.Math.PI);
			}
			//If it's the star, rotate it about the Y axis
			else if (i == 6 || i ==7){
				material = 1;
				mMat.rotateY((float)Math.toRadians(-rotationSpeed * 5 * deltaTime));
			}
			else if (i == 3){
				mMat.rotateY((float)Math.toRadians(rotationSpeed * 5 * deltaTime));
			}
			//If it is the eye, set its location to the light's position
			else if (i == 5){
				material = 1;
				mMat.setColumn(3, new Vector4f(currentLightPos.x, currentLightPos.y, currentLightPos.z, 1.0f));
			}

			mMat.invert(invTrMat);
			invTrMat.transpose(invTrMat);
			modelMatrices[i] = mMat;
			invTrsMatrices[i] = invTrMat;

		}
	}
	
	private void setupShadowBuffers()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		scSizeX = myCanvas.getWidth();
		scSizeY = myCanvas.getHeight();
	
		gl.glGenFramebuffers(1, shadowBuffer, 0);
	
		gl.glGenTextures(1, shadowTex, 0);
		gl.glBindTexture(GL_TEXTURE_2D, shadowTex[0]);
		gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32,
						scSizeX, scSizeY, 0, GL_DEPTH_COMPONENT, GL_FLOAT, null);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
		
		// may reduce shadow border artifacts
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	}

	private void setupVertices()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		
		//Buffers
		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);

		gl.glGenBuffers(vbo.length, vbo, 0);
		gl.glGenBuffers(skyboxVBO.length, skyboxVBO, 0);

		for (int k = 0; k < numOfModels; k++){
			numObjVertices = models[k].getNumVertices();
			vertices = models[k].getVertices();
			texCoords = models[k].getTexCoords();
			normals = models[k].getNormals();

			pvalues = new float[numObjVertices*3];
			tvalues = new float[numObjVertices*2];
			nvalues = new float[numObjVertices*3];
			
			for (int i = 0; i < numObjVertices; i++){	
				pvalues[i*3]   = (float) (vertices[i]).x();
				pvalues[i*3+1] = (float) (vertices[i]).y();
				pvalues[i*3+2] = (float) (vertices[i]).z();
				tvalues[i*2]   = (float) (texCoords[i]).x();
				tvalues[i*2+1] = (float) (texCoords[i]).y();
				nvalues[i*3]   = (float) (normals[i]).x();
				nvalues[i*3+1] = (float) (normals[i]).y();
				nvalues[i*3+2] = (float) (normals[i]).z();
			}

			allpvalues.add(k, pvalues);
			alltvalues.add(k, tvalues);
			allnvalues.add(k, nvalues);

			//Vertices
			gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[numOfBuffersPerObject*k]);
			vertBuf = Buffers.newDirectFloatBuffer(pvalues);
			gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL_STATIC_DRAW);

			//Texture Coordinates
			gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[(numOfBuffersPerObject*k)+1]);
			texBuf = Buffers.newDirectFloatBuffer(tvalues);
			gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit()*4, texBuf, GL_STATIC_DRAW);
			
			//Normal Vectors
			gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[(numOfBuffersPerObject*k)+2]);
			norBuf = Buffers.newDirectFloatBuffer(nvalues);
			gl.glBufferData(GL_ARRAY_BUFFER, norBuf.limit()*4,norBuf, GL_STATIC_DRAW);
		}



		//Skybox
		// cube
		float[] cubeVertexPositions =
		{	-1.0f,  1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f,
			1.0f, -1.0f, -1.0f, 1.0f,  1.0f, -1.0f, -1.0f,  1.0f, -1.0f,
			1.0f, -1.0f, -1.0f, 1.0f, -1.0f,  1.0f, 1.0f,  1.0f, -1.0f,
			1.0f, -1.0f,  1.0f, 1.0f,  1.0f,  1.0f, 1.0f,  1.0f, -1.0f,
			1.0f, -1.0f,  1.0f, -1.0f, -1.0f,  1.0f, 1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f,  1.0f, -1.0f,  1.0f,  1.0f, 1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f,  1.0f, -1.0f, -1.0f, -1.0f, -1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f, -1.0f, -1.0f,  1.0f, -1.0f, -1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f,  1.0f,  1.0f, -1.0f,  1.0f,  1.0f, -1.0f, -1.0f,
			1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f,  1.0f,
			-1.0f,  1.0f, -1.0f, 1.0f,  1.0f, -1.0f, 1.0f,  1.0f,  1.0f,
			1.0f,  1.0f,  1.0f, -1.0f,  1.0f,  1.0f, -1.0f,  1.0f, -1.0f
		};

		gl.glBindBuffer(GL_ARRAY_BUFFER, skyboxVBO[0]);
		FloatBuffer cvertBuf = Buffers.newDirectFloatBuffer(cubeVertexPositions);
		gl.glBufferData(GL_ARRAY_BUFFER, cvertBuf.limit()*4, cvertBuf, GL_STATIC_DRAW);
	}
	
	private void installLights(int renderingProgram)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();

		lightPos[0]=currentLightPos.x(); 
		lightPos[1]=currentLightPos.y(); 
		lightPos[2]=currentLightPos.z();
		
		// set current material values
		matAmb = thisAmb;
		matDif = thisDif;
		matSpe = thisSpe;
		matShi = thisShi;
		
		// get the locations of the light and material fields in the shader
		globalAmbLoc = gl.glGetUniformLocation(renderingProgram, "globalAmbient");
		ambLoc = gl.glGetUniformLocation(renderingProgram, "light.ambient");
		diffLoc = gl.glGetUniformLocation(renderingProgram, "light.diffuse");
		specLoc = gl.glGetUniformLocation(renderingProgram, "light.specular");
		posLoc = gl.glGetUniformLocation(renderingProgram, "light.position");
		mambLoc = gl.glGetUniformLocation(renderingProgram, "material.ambient");
		mdiffLoc = gl.glGetUniformLocation(renderingProgram, "material.diffuse");
		mspecLoc = gl.glGetUniformLocation(renderingProgram, "material.specular");
		mshiLoc = gl.glGetUniformLocation(renderingProgram, "material.shininess");
	
		//  set the uniform light and material values in the shader
		gl.glProgramUniform4fv(renderingProgram, globalAmbLoc, 1, globalAmbient, 0);
		gl.glProgramUniform4fv(renderingProgram, ambLoc, 1, lightAmbient, 0);
		gl.glProgramUniform4fv(renderingProgram, diffLoc, 1, lightDiffuse, 0);
		gl.glProgramUniform4fv(renderingProgram, specLoc, 1, lightSpecular, 0);
		gl.glProgramUniform3fv(renderingProgram, posLoc, 1, lightPos, 0);
		gl.glProgramUniform4fv(renderingProgram, mambLoc, 1, matAmb, 0);
		gl.glProgramUniform4fv(renderingProgram, mdiffLoc, 1, matDif, 0);
		gl.glProgramUniform4fv(renderingProgram, mspecLoc, 1, matSpe, 0);
		gl.glProgramUniform1f(renderingProgram, mshiLoc, matShi);
	}

	//Calculates the amount of time between the current frame and previous frame, sets deltaTime to the difference
	public float calculateDeltaTime(){
		if (lastTime == 0L)
			lastTime = System.currentTimeMillis();
		curTime = System.currentTimeMillis();
		deltaTime = (float)(curTime - lastTime);
		lastTime = curTime;
		return deltaTime;
	}

	//Getters and setters
	public Vector3f getWorldRightVector(){
		return worldRightVector;
	}

	public Vector3f getWorldUpVector(){
		return worldUpVector;
	}

	public Vector3f getWorldForwardVector(){
		return worldForwardVector;
	}

	public Vector3f getWorldOrigin(){
		return worldOrigin;
	}

	//KeyListener implemented functions
	@Override
	public void keyPressed(KeyEvent e){	
		switch (e.getKeyCode()){
			case KeyEvent.VK_W:
				//Move camera forward
				cam.moveAlongN(movementSpeed);
				break;
			case KeyEvent.VK_S:
				//Move camera backward
				cam.moveAlongN(-movementSpeed);
				break;
			case KeyEvent.VK_A:
				//Move camera left
				cam.moveAlongU(-movementSpeed);
				break;
			case KeyEvent.VK_D:
				//Move camera right
				cam.moveAlongU(movementSpeed);
				break;
			case KeyEvent.VK_Q:
				//Move camera up
				cam.moveAlongV(movementSpeed);
				break;
			case KeyEvent.VK_E:
				//Move camera down
				cam.moveAlongV(-movementSpeed);
				break;
			case KeyEvent.VK_UP:
				//Rotate camera up (pitch up)
				cam.pitch(pitchAmount);
				break;
			case KeyEvent.VK_DOWN:
				//Rotate camera down (pitch down)
				cam.pitch(-pitchAmount);
				break;
			case KeyEvent.VK_LEFT:
				//Rotate camera left (yaw left)
				cam.yaw(panAmount);
				break;
			case KeyEvent.VK_RIGHT:
				//Rotate camera right (yaw right)
				cam.yaw(-panAmount);
				break;
			case KeyEvent.VK_SPACE:
				//Toggle world axis
				areAxesVisible = !areAxesVisible;
				break;
			case KeyEvent.VK_COMMA:
				//Move light back
				amt -= deltaTime * 0.09f;
				break;
			case KeyEvent.VK_PERIOD:
				//Move light forward
				amt += deltaTime * 0.09f;
				break;
		}
	}

	//Required implementations, but not used
	@Override
	public void keyReleased(KeyEvent e){}

	@Override
	public void keyTyped(KeyEvent e){}

	public static void main(String[] args) { new Code(); }
	public void dispose(GLAutoDrawable drawable) {}

	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();

		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

		setupShadowBuffers();
	}
}