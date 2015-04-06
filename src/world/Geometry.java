package world;

public class Geometry
{
    public static float[] calculate_normal(float[] po, float[] pi, float[] pj)
    {
        float ix = pi[0] - po[0];
        float iy = pi[1] - po[1];
        float iz = pi[2] - po[2];
        
        float jx = pj[0] - po[0];
        float jy = pj[1] - po[1];
        float jz = pj[2] - po[2];
        
        float[] n = new float[]{
                iy * jz - iz * jy,
                iz * jx - ix * jz,
                ix * jy - iy * jx
        };
        return n;
    }

    public static boolean same_vector(float[] pa, float[] pb)
    {
        return (pa[0] == pb[0]) && (pa[1] == pb[1]) && (pa[2] == pb[2]);
    }
    
    public static float linear_interpolation(float pa, float pb,
                                             float delta, float len)
    {
        return (delta / len) * Math.abs(pa - pb) + Math.min(pa, pb);
    }
    
    public static float cosinus_interpolation(float pa, float pb, float delta,
                                              float len)
    {
        float direction = (pa > pb) ? (float) Math.PI : 0;
        float semi_altitude = Math.abs(pa-pb)/2;
       
        return (float) (semi_altitude *
                        Math.cos(direction + Math.PI * delta / len) +
                        semi_altitude + Math.min(pa, pb));
    }
    
    public static float distance_square(float[] pa, float[] pb)
    {
        return ((pa[0] - pb[0]) * (pa[0] - pb[0]) +
                (pa[1] - pb[1]) * (pa[1] - pb[1]) +
                (pa[2] - pb[2]) * (pa[2] - pb[2]));
    }

    public static float value_step(float actual, float finish, float step)
    {
        float value = finish;
        if (Math.abs(finish - actual) > step)
            value = actual + step * ((finish > actual) ? 1 : -1);
        return value;
    }
}

