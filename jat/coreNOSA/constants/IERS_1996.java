package jat.coreNOSA.constants;

public interface IERS_1996 {
    /** Speed of Light in m/s
     */
    public static final double c = 299792458.0;
    /** Obliquity of the Ecliptic at J2000 in arcsec
     */
    public static final double e0 = 84381.412;
    /** Mean Earth Radius in m
     */
    public final static double R_Earth = 6378136.49;    
    /** Earth gravity constant in m^3/s^2
     */    
    public static final double GM_Earth = 3.986004418E14;
    /** Earth's rotation rate in rad/s.
     */    
    public static final double w_Earth = 7.292115E-05;    

    public static final double J2_Earth = 1.0826359E-03;
    /** Flattening factor of earth
     */
    public final static double f_Earth = 1.0/298.25642;
    /** Earth mean equatorial gravity in m/s^2
     */
    public static final double g_Earth = 9.780327;
    /** Sun gravity constant in m^3/s^2
     */        
    public static final double GM_Sun = 1.327124E20;
    /** Moon - Earth mass ratio
     */
    public static final double Moon_Earth_Ratio = 0.0123000345;
    
    
}
