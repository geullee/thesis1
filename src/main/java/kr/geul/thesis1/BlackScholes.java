package kr.geul.thesis1;

public class BlackScholes {

    public static double getCallPrice(double S, double K, double r, double sigma, double tau) {
        double d1 = (Math.log(S / K) + (r + sigma * sigma / 2.0) * tau) / (sigma * Math.sqrt(tau));
        double d2 = d1 - sigma * Math.sqrt(tau);
        return S * Gaussian.Phi(d1) - K * Math.exp(-r * tau) * Gaussian.Phi(d2);
    }
	
    public static double getPutPrice(double S, double K, double r, double sigma, double tau) {
        double d1 = (Math.log(S / K) + (r + sigma * sigma / 2.0) * tau) / (sigma * Math.sqrt(tau));
        double d2 = d1 - sigma * Math.sqrt(tau);
        return K * Math.exp(-r * tau) * Gaussian.Phi(-d2) - S * Gaussian.Phi(-d1) ;
    }
    
    public static double getCallPriceWithDividend(double S, double K, double r, double dividend,
    		double sigma, double tau) {
        double d1 = (Math.log(S / K) + ((r - dividend) + sigma * sigma / 2.0) * tau) / (sigma * Math.sqrt(tau));
        double d2 = d1 - sigma * Math.sqrt(tau);
        return S * Math.exp(-dividend * tau) * Gaussian.Phi(d1) - K * Math.exp(-r * tau) * Gaussian.Phi(d2);
    }
	
    public static double getPutPriceWithDividend(double S, double K, double r, double dividend, 
    		double sigma, double tau) {
        double d1 = (Math.log(S / K) + ((r - dividend) + sigma * sigma / 2.0) * tau) / (sigma * Math.sqrt(tau));
        double d2 = d1 - sigma * Math.sqrt(tau);
        return K * Math.exp(-r * tau) * Gaussian.Phi(-d2) - S * Math.exp(-dividend * tau) * Gaussian.Phi(-d1) ;
    }
    
}
