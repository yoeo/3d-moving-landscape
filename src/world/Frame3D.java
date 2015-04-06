package world;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
 
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.fixedfunc.GLMatrixFunc;
import javax.media.opengl.glu.GLU;
 
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.gl2.GLUT;
 
public class Frame3D implements GLEventListener, KeyListener
{
    float rx = 0;
    float ry = 0;
    float scale = 0;
    LandMap map = null;

    int text_height = 0;
    
    GLU glu = new GLU(); 
    GLCanvas canvas = new GLCanvas();
    Frame frame = new Frame("Land viewer");
    Animator animator = new Animator(canvas);
    
    public Frame3D()
    {
        // map
        this.map = new LandMap(30, 30, 3);
        this.map.refresh();
        
        this.rx = 20;
        this.ry = 20;
        this.scale = 10;
    }
    
    public void init(GLAutoDrawable gLDrawable)
    {
        GL2 gl = gLDrawable.getGL().getGL2();

        // OpenGL initialization
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glClearDepth(1.0f);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthFunc(GL.GL_LEQUAL);
        gl.glHint(GL2ES1.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
        ((Component) gLDrawable).addKeyListener(this);
    }
 
    public void display(GLAutoDrawable gLDrawable)
    {
        final GL2 gl = gLDrawable.getGL().getGL2();
        
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
        
        gl.glLoadIdentity();
        gl.glTranslatef(0.0f, 0.0f, -5.0f);

        // rotate on the axis
        gl.glRotatef(this.rx, 1.0f, 0.0f, 0.0f);
        gl.glRotatef(this.ry, 0.0f, 1.0f, 0.0f);
        gl.glScalef(1.0f / this.scale, 1.0f / this.scale, 1.0f / this.scale);
 
        threadsafe_draw_map(gl);
        draw_origin(gl, false);
        draw_text(gl);
    }

    private void draw_origin(GL2 gl, boolean realy_draw_it)
    {
        if (!realy_draw_it) return;
        
        gl.glBegin(GL2.GL_LINES);
        
        gl.glColor3f(1.0f, 0.0f, 0.0f);
        gl.glVertex3f(0.0f, 0.0f, 0.0f);
        gl.glVertex3f(200.0f, 0.0f, 0.0f);

        gl.glColor3f(0.0f, 1.0f, 0.0f);
        gl.glVertex3f(0.0f, 0.0f, 0.0f);
        gl.glVertex3f(0.0f, 200.0f, 0.0f);

        gl.glColor3f(0.0f, 0.0f, 1.0f);
        gl.glVertex3f(0.0f, 0.0f, 0.0f);
        gl.glVertex3f(0.0f, 0.0f, 200.0f);
        gl.glEnd();
    }

    private synchronized void threadsafe_draw_map(GL2 gl)
    {
        float[] size = this.map.get_size();
        gl.glTranslatef(-size[0] / 2, -size[1] / 2, -size[2] / 2);

        gl.glBegin(GL2.GL_TRIANGLES);
        
        int[] triangle = null;
        for (int n = 0; n < this.map.triangle.length; n++)
        {
            triangle = this.map.triangle[n];
            gl.glColor3fv(this.map.color[triangle[0]], 0);
            gl.glVertex3fv(this.map.vertex[triangle[0]], 0);
            gl.glColor3fv(this.map.color[triangle[1]], 0);
            gl.glVertex3fv(this.map.vertex[triangle[1]], 0);
            gl.glColor3fv(this.map.color[triangle[2]], 0);
            gl.glVertex3fv(this.map.vertex[triangle[2]], 0);
        }
        gl.glEnd();
        this.map.morphing_step();
    }

    private void draw_text(GL2 gl)
    {
        // Text
        final GLUT glut = new GLUT();
        double c = this.map.get_cut_coef();
        String[] text = new String[]{
            "___________________________________________",
            "How you use it:",
            "( LEFT | RIGHT | UP | DOWN ) Rotate the map",
            "( + | - ) Zoom in and zoom out",
            "( C ) Cut the map with a random vector -> y = " + c + " * x)",
            "( S ) Show the whole map",
            "( SPACE ) Morphing, animate the map",
            "( R ) Reset the map",
            "( ESC ) Exit...",
            "___________________________________________"
        };
        
        int height = this.text_height - 20;
        gl.glColor3f(0.5f, 0.8f, 0.8f);
        for (String line : text)
        {
            gl.glWindowPos2f(10, height);
            glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, line);
            height -= 20;
        }
    }

    public void reshape(GLAutoDrawable gLDrawable, int x, int y, int width, int height)
    {
        GL2 gl = gLDrawable.getGL().getGL2();
        if (height <= 0) {
            height = 1;
        }
        float h = (float) width / (float) height;
        gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(50.0f, h, 1.0, 1000.0);
        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        gl.glLoadIdentity();
        
        this.text_height = height;
    }
 
    public void keyPressed(KeyEvent e)
    {
        float step = 1f;
        float scale_step = 1.01f;
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
            exit();
        // left right
        else if (e.getKeyCode() == KeyEvent.VK_LEFT)
            this.ry += step;
        else if (e.getKeyCode() == KeyEvent.VK_RIGHT)
            this.ry -= step;
        // up down
        else if (e.getKeyCode() == KeyEvent.VK_UP)
            this.rx += step;
        else if (e.getKeyCode() == KeyEvent.VK_DOWN)
            this.rx -= step;
        // + -
        else if (e.getKeyChar() == '+')
            this.scale /= scale_step;
        else if (e.getKeyChar() == '-' && this.scale < 20)
            this.scale *= scale_step;
        // r
        else if ((e.getKeyChar() == 'r') || (e.getKeyChar() == 'R'))
            this.threadsafe_refresh_map();
        // c
        else if ((e.getKeyChar() == 'c') || (e.getKeyChar() == 'C'))
            this.threadsafe_cut_map();
        // s
        else if ((e.getKeyChar() == 's') || (e.getKeyChar() == 'S'))
            this.threadsafe_uncut_map();
        // space
        else if (e.getKeyChar() == ' ')
            this.map.begin_morphing();
    }
 
    private synchronized void threadsafe_uncut_map()
    {
        this.map.clear_cut();
    }

    private synchronized void threadsafe_cut_map()
    {
        this.map.random_cut();
    }

    private synchronized void threadsafe_refresh_map()
    {
        this.map.refresh();        
    }

    public void displayChanged(GLAutoDrawable gLDrawable, boolean modeChanged,
                               boolean deviceChanged){}
    public void keyReleased(KeyEvent e){}
    public void keyTyped(KeyEvent e){}
    public void glutPassiveMotionFunc(){}
    public void dispose(GLAutoDrawable gLDrawable){}
    
    public void exit()
    {
        animator.stop();
        frame.dispose();
        System.exit(0);
    }
 
    public void start()
    {
        canvas.addGLEventListener(this);
        frame.add(canvas);
        frame.setSize(800, 600);
        //frame.setUndecorated(true);
        //frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        frame.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                exit();
            }
        });
        
        frame.setVisible(true);
        animator.start();
        canvas.requestFocus();

        this.map.begin_morphing();
    }
}
