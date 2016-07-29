package jat.coreNOSA.constants;

public interface WGS84 {
    /** Mean Earth Radius in km from WGS-84.
     */
    public final static double R_Earth = 6378.137e3;      // Radius Earth [m]; WGS-84
    /** Flattening factor of earth from WGS-84
     */
    public final static double f_Earth = 1.0/298.257223563; // Flattening; WGS-84
    /** Earth gravity constant in m^3/s^2 from WGS84
     */
    public final static double GM_Earth    = 398600.5e+9;    // [m^3/s^2]; WGS-84

    /** Earth's rotation rate in rad/s.
     */
    public final static double omega_Earth = 7.2921151467E-05;  // earth rotation rate

}
