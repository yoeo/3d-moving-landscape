package world;

import java.util.Random;

public class LandMap
{
    public float[][] vertex = null;
    public int[][] triangle = null;
    public float[][] color = null;

    private float[][] real_vertex = null;
    private int[][] real_triangle = null;
    private float real_max_y = 0;
    
    private int nb_level = 0;
    private int max_x = 0;
    private int max_z = 0;
    private float max_y = 0;

    private int triangle_counter = 0;
    private float cut_coef = 0;

    private float morphing_step_value = 0;
    private boolean morphing_in_process = false;
    
    float[] light_ray = null; 

    public LandMap(int max_x, int max_z, int level_nb)
    {
        this.nb_level = level_nb;
        this.max_x = max_x;
        this.max_z = max_z;
    
        Random r = new Random();
        this.cut_coef = r.nextFloat();
        this.morphing_step_value = 0.2f;
        this.morphing_in_process = false;
        this.light_ray = new float[]{1, 0, 0};
        
        this.real_vertex = new float[][]{};
        this.real_triangle = new int[][]{};
        
        this.vertex = new float[][]{};
        this.triangle = new int[][]{};
        this.color = new float[][]{};
    }
    
    public void refresh()
    {
        // Perlin like algorithm
        this.build_map();
        // Delaunay like algorithm
        this.delauney();

        this.update_triangle_color();
        this.vertex = this.real_vertex;
        this.triangle = this.real_triangle;
    }

    private void update_triangle_color()
    {
        float[] normal = null;
        float reflected_light = 0; 

        for (int n = 0; n < this.triangle.length; n++)
        {
            normal = Geometry.calculate_normal(
                    this.vertex[this.triangle[n][0]],
                    this.vertex[this.triangle[n][1]],
                    this.vertex[this.triangle[n][2]]);
            for (int coord = 0; coord < 3; coord++)
            {
                this.color[this.triangle[n][0]][coord] += normal[coord];
                this.color[this.triangle[n][1]][coord] += normal[coord];
                this.color[this.triangle[n][2]][coord] += normal[coord];
            }
        }
        
        for (int n = 0; n < this.color.length; n++)
        {
            reflected_light = (
                    this.color[n][0] * this.light_ray[0] +
                    this.color[n][1] * this.light_ray[1] +
                    this.color[n][2] * this.light_ray[2]);
            reflected_light /= Math.sqrt(
                    this.color[n][0] * this.color[n][0] +
                    this.color[n][1] * this.color[n][1] +
                    this.color[n][2] * this.color[n][2]) + 0.001f; 
            reflected_light = 0.5f + reflected_light / 2f;
            
            this.color[n][0] = (float)Math.max(reflected_light * 0.9f + 0.1,
                                               0.2);
            this.color[n][1] = (float)Math.max(reflected_light * 1.3, 0.2);
            this.color[n][2] = (float)Math.max(reflected_light * 0.6f + 0.2,
                                               0.2);
        }

    }

    private void create_tesselation_vertex(int max_x, int max_z,
                                           float[][] vertex)
    {
        float[] p00 = null;
        float[] p10 = null;
        float[] p01 = null;
        float[] p11 = null;
        
        float[] p = null;
        float left = 0;
        float right = 0;
        
        Random r = new Random();
        for (int i = 0; i < max_x - 1; i++)
        {
            for (int j = 0; j < max_z - 1; j++)
            {
                // get a random intermediate vertex and add it
                p00 = this.get_vertex(vertex, i, j);
                p01 = this.get_vertex(vertex, i, j + 1);
                p10 = this.get_vertex(vertex, i + 1, j);
                p11 = this.get_vertex(vertex, i + 1, j + 1);

                int p_index = j + i * (max_z - 1) + (max_x * max_z);
                p = vertex[p_index];
                p[0] = r.nextFloat();
                p[1] = 0;
                p[2] = r.nextFloat();
                left  = Geometry.cosinus_interpolation(p00[1], p10[1], p[0], 1); 
                right = Geometry.cosinus_interpolation(p01[1], p11[1], p[0], 1); 
                p[1]  = Geometry.cosinus_interpolation(left, right, p[2], 1);
                p[0] += i;
                p[2] += j;
            }
        }
    }

    public void begin_morphing()
    {
        this.build_map();
        this.morphing_in_process = true;
    }

    public float get_cut_coef()
    {
        return (float) (Math.round(100 * this.cut_coef) / 100.0);
    }
    
    public void clear_cut()
    {
        this.triangle = this.real_triangle;
        this.update_triangle_color();
    }

    public void random_cut()
    {
        this.triangle = new int[this.real_triangle.length][3];
        int[] empty = new int[]{0, 0, 0};
        for (int n = 0; n < this.triangle.length; n++)
        {
            if (this.real_vertex[real_triangle[n][0]][2] >
                this.real_vertex[real_triangle[n][0]][0] * 5 * this.cut_coef)
                this.triangle[n] = empty;
            else
                this.triangle[n] = this.real_triangle[n];
        }
        this.update_triangle_color();
        Random r = new Random();
        this.cut_coef = r.nextFloat();
    }
    
    private void build_map()
    {
        int nb_vertex = (this.max_x * this.max_z) +
                         ((this.max_x - 1) * (this.max_z - 1));
        this.real_vertex = this.perlin(this.max_x, this.max_z, this.nb_level,
                                       nb_vertex);
        this.create_tesselation_vertex(this.max_x, this.max_z,
                                       this.real_vertex);
        this.color = new float[nb_vertex][3];
        this.update_triangle_color(); // to avoid black screen flash

        // height calculation
        float max = 0;
        float min = 0;
        for (int n = 0; n < this.real_vertex.length; n++)
        {
            max = Math.max(max, this.real_vertex[n][1]);
            min = Math.min(min, this.real_vertex[n][1]);
        }
        for (int n = 0; n < this.real_vertex.length; n++)
            this.real_vertex[n][1] -= min;
        this.real_max_y = max - min;
    }

    // Perlin mapping
    private float[][] perlin(int max_x, int max_z, int nb_level, int nb_vertex)
    {
        Random r = new Random();
        int step = (int) Math.pow(2, nb_level);
        float[][] vertex_list = new float[nb_vertex][3];
        float[] current_vertex = null;
        for (int i = 0; i < max_x; i ++)
            for (int j = 0; j < max_z; j ++)
            {
                current_vertex = this.get_vertex(vertex_list, i, j);
                current_vertex[0] = i;
                current_vertex[1] = 0;
                current_vertex[2] = j;
            }
        // map generation 
        float[][] layer = new float[max_x][max_z];
        for (int n = 0; (n < nb_level); n++)
        {
            for (int i = 0; i < max_x; i += step)
                for (int j = 0; j < max_z; j += step)
                    layer[i][j] = r.nextFloat();
            for (int i = 0; i < max_x; i++)
                for (int j = 0; j < max_z; j++)
                    this.get_vertex(vertex_list, i, j)[1] += (
                        this.interpolate_layer(i, j, step, layer) * step);
            if (step <= 1)
                break;
            else
                step = step / 2;
        }
        return vertex_list;
    }
    
    // Delauney triangulation
    private void delauney()
    {
        float[] p00 = null;
        float[] p10 = null;
        float[] p01 = null;
        float[] p11 = null;
        
        float[] p = null;
        float[] p_right = null;
        float[] p_bottom = null;

        int nb_triangle = (this.max_x - 1) * (this.max_z - 1) * 4;
        this.real_triangle = new int[nb_triangle][3];
        this.triangle_counter = 0;
        
        for (int i = 0; i < this.max_x - 1; i++)
        {
            for (int j = 0; j < this.max_z - 1; j++)
            {
                // get a random intermediate vertex and add it
                p00 = this.get_vertex(this.real_vertex, i, j);
                p01 = this.get_vertex(this.real_vertex, i, j + 1);
                p10 = this.get_vertex(this.real_vertex, i + 1, j);
                p11 = this.get_vertex(this.real_vertex, i + 1, j + 1);

                int p_index = j + i * (this.max_z - 1) + (this.max_x *
                                                          this.max_z);
                p = this.real_vertex[p_index];
                // add triangles and normals

                // first column (left)
                if (j == 0)
                    this.add_triangle(p, p10, p00);

                // last column (right)
                if (j == (this.max_z - 2))
                    this.add_triangle(p, p01, p11);

                // the 2 right triangles.
                p_right = this.real_vertex[p_index - 1];
                // the shortest segment will be used for both triangles
                if (Geometry.distance_square(p, p_right) >
                    Geometry.distance_square(p00, p10))
                {
                    // the shortest segment is p_left --- p
                    if (j > 0)
                    {
                        this.add_triangle(p_right, p00, p10);
                        this.add_triangle(p, p10, p00);
                    }
                }
                else
                {
                    // the shortest segment is p00 --- p01
                    this.add_triangle(p, p10, p_right);
                    this.add_triangle(p, p_right, p00);
                }

                // last line (top)
                if (i >= (this.max_x - 2))
                    this.add_triangle(p, p11, p10);

                // first line (bottom)
                if (i <= 0)
                    this.add_triangle(p, p00, p01);
                // the 2 bottom triangles
                else
                {
                    p_bottom = this.real_vertex[p_index - this.max_z + 1];
                    // the shortest segment will be used
                    if (Geometry.distance_square(p, p_bottom) >
                        Geometry.distance_square(p00, p01))
                    {
                        // the shortest segment is p_bottom --- p
                        this.add_triangle(p_bottom, p01, p00);
                        this.add_triangle(p, p00, p01) ;
                    }
                    else
                    {
                        // the shortest segment is p00 --- p10
                        this.add_triangle(p, p00, p_bottom);
                        this.add_triangle(p, p_bottom, p01);
                    }
                }
            }
        }
    }

    private void add_triangle(float[] o, float[] p0, float[] p1)
    {
        int[] index = new int[]{0, 0, 0};
        for (int n = 0; n < this.real_vertex.length; n++)
        {
            if (Geometry.same_vector(o, this.real_vertex[n]))
                index[0] = n;
            else if (Geometry.same_vector(p0, this.real_vertex[n]))
                index[1] = n;
            else if (Geometry.same_vector(p1, this.real_vertex[n]))
                index[2] = n;
        }
        this.real_triangle[this.triangle_counter] = index;
        this.triangle_counter++;
    }
    
    public float[] get_size()
    {
        return new float[]{this.max_x, this.max_y, this.max_z};
    }
    
    private float[] get_vertex(float[][] vertex_list, int i, int j)
    {
        float[] value = null;
        if ((i * this.max_z + j) < vertex_list.length)
            value = vertex_list[i * this.max_z + j];
        return value;
    }
    
    private float interpolate_layer(int i, int j, int step, float[][] layer)
    {
        float value = 0;

        int base_i = i - (i % step);
        int base_j = j - (j % step);
        int base_i_1 = ((base_i + step) < this.max_x) ? base_i + step : base_i;
        int base_j_1 = ((base_j + step) < this.max_z) ? base_j + step : base_j;

        if ((i == base_i) && (j == base_j))
        {
            value = layer[i][j];
        }
        else
        {
            float p00 = layer[base_i  ][base_j];
            float p01 = layer[base_i  ][base_j_1];
            float p10 = layer[base_i_1][base_j];
            float p11 = layer[base_i_1][base_j_1];

            float delta_i = step - (i % step);
            float delta_j = step - (j % step);
            float pi0 = Geometry.cosinus_interpolation(p00, p10, delta_i, step);
            float pi1 = Geometry.cosinus_interpolation(p01, p11, delta_i, step);
            value = Geometry.cosinus_interpolation(pi0, pi1, delta_j, step);
        }
        return value;
    }

    public void morphing_step()
    {
        if (this.morphing_in_process)
        {
            boolean continue_morphing = false;
            // update vertex
            for (int n = 0; n < this.vertex.length; n++)
            {
                if (!Geometry.same_vector(this.vertex[n], this.real_vertex[n]))
                {
                    this.vertex[n][0] = Geometry.value_step(
                        this.vertex[n][0], this.real_vertex[n][0],
                        this.morphing_step_value);
                    this.vertex[n][1] = Geometry.value_step(
                        this.vertex[n][1], this.real_vertex[n][1],
                        this.morphing_step_value);
                    this.vertex[n][2] = Geometry.value_step(this.vertex[n][2],
                        this.real_vertex[n][2], this.morphing_step_value);
                    continue_morphing = true;
                }
            }
            
            if (this.max_y != this.real_max_y)
            {
                this.max_y = Geometry.value_step(
                    this.max_y, this.real_max_y, this.morphing_step_value);
                continue_morphing = true;
            }
            
            if (continue_morphing)
                this.update_triangle_color();
            else
                this.morphing_in_process = false;

        }
    }
}

