import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.jtransforms.fft.DoubleFFT_1D;

public class FFT_from_NeuroNicle_Raw {
	
	private static double[] rawDataStream = new double[7500];		// FFT�� �ϱ� ���� �迭 ���� (250 * 30�� = 7500)
	private static double[] result = new double[12];				// ���� �Ŀ�����Ʈ�� ��� ��, ǥ������, ����

	public static void main(String[] args) {

		for (int n = 1 ; n <= 20 ; n++) {
		
			String fileNum = "";
			if (n / 10 == 0)	fileNum = "0" + n;					// ���� �ڸ�
			else				fileNum = String.valueOf(n);		// ���� �ڸ�					
			
			File rawFile = new File("../NeuroDataConverterP/NeuroNicleRaw/NeuroNicle_" + fileNum + "_1.csv");
			File fftFile = new File("../NeuroDataConverterP/NeuroNicleFFT/NeuroNicleFFT_" + fileNum + "_1.csv");
			
			System.out.println("NeuroNicleRaw/NeuroNicle_" + fileNum + "_1.csv file read and run FFT...");
			
			BufferedReader br = null;
			BufferedWriter bw = null;
			try {
				br = new BufferedReader(new FileReader(rawFile));
				bw = new BufferedWriter(new FileWriter(fftFile, true));
				
				String rawData;
				int dataCounter = 0;
				while ((rawData = br.readLine()) != null) {					
					
					if (dataCounter < 7500 ) {						// 30�ʵ����� �����͸� �ϴ� ������
						
						rawDataStream[dataCounter++] = Double.parseDouble(rawData);
						
					} else if (dataCounter == 7500) {				// 30�ʵ����� �����͸� �� �������						
						
						dataCounter = 7250;							// 1�ʵ��� ���̴� �����͸� ����(=250)							
						
						// FFT ����
						DoubleFFT_1D fft_LCh1D = new DoubleFFT_1D(rawDataStream.length);
                        double[] fft_LCh = new double[rawDataStream.length * 2];

                        System.arraycopy(rawDataStream, 0, fft_LCh, 0, rawDataStream.length);
                        fft_LCh1D.realForwardFull(fft_LCh);

                        // ���ļ� �� �Ŀ�����Ʈ�� �� ���ϱ�
                        double realValue_l, imagValue_l, psValue_l;
                        double [] delta = new double[126];
                        double [] theta = new double[133];
                        double [] alpha = new double[167];
                        double [] beta = new double[567];
                        int delta_cnt = 0, theta_cnt = 0, alpha_cnt = 0, beta_cnt = 0;

                        for (int i = 2; i < fft_LCh.length - 1; i += 2) {
                            realValue_l = fft_LCh[i];
                            imagValue_l = fft_LCh[i + 1];

                            psValue_l = Math.sqrt(Math.pow(realValue_l, 2) + Math.pow(imagValue_l, 2));

                            if (i >= 14 && i < 266 ) {												// Delta ����: 14/2 = 7(0.21Hz) ~ 266/2 = 133(3.99Hz)
                                delta[delta_cnt++] = psValue_l;
                            } else if (i >= 266 && i < 532) {										// Theta ����: 266/2 = 133(3.99Hz) ~ 532/2 = 266(�� 7.99Hz)
                                theta[theta_cnt++] = psValue_l;
                            } else if (i >= 532 && i < 866) {										// Alpha ����: 532/2 = 266(�� 7.99Hz) ~ 866/2 = 433(12.99Hz)
                                alpha[alpha_cnt++] = psValue_l;
                            } else if (i >= 866 && i < 2000) {										// Beta ����: 866/2 = 433(12.99Hz) ~ 2000/2 = 1000(30Hz)
                                beta[beta_cnt++] = psValue_l;
                            }
                        }

                        // ���ļ� �� �Ŀ�����Ʈ�� ��� ��, ǥ������, ���� ���ϱ�
                        result = calcMetrics(delta, theta, alpha, beta);
                        
                    	DecimalFormat psWaveFormatter = new DecimalFormat("#0.000000");				// �Ҽ��� ���� 6�ڸ��� ǥ��                    	
                        
                        bw.write("delta mean,delta std,theta mean,theta std,alpha mean,alpha std,beta mean,beta std,alpha/theta,alpha/delta,theta/delta,beta/delta\n");
                    	for (int j = 0 ; j < result.length ; j++)
                        	bw.write(psWaveFormatter.format(result[j]) + ',');
                        bw.write('\n');
                        bw.flush();
						
						// �迭�� �� 1�ʰ�(250��) ������ ������ �������� �̵��ϱ�
						System.arraycopy(rawDataStream, 250, rawDataStream, 0, 250);
					}
					
				}
				
			} catch (FileNotFoundException fnfe) {
				System.out.println("NeuroNicle_" + fileNum + "_1.csv ������ �����ϴ�.");
			} catch (IOException ioe) {
				ioe.printStackTrace();
			} finally {
				try {
					if (bw != null)	bw.close();
					if (br != null) br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}				
			}
			
		}
		
	}
	
	public static double getMean(double [] array) {
        double totalSum = 0.0;

        for (int i = 0 ; i < array.length ; i++)
            totalSum += array[i];

        return totalSum/array.length;
    }

    public static double getStdev(double [] array, double mean) {
        double sum = 0.0;
        double sd = 0.0;
        double diff;

        for (int i = 0 ; i < array.length ; i++) {
            diff = array[i] - mean;
            sum += diff * diff;
        }

        sd = Math.sqrt(sum / array.length);

        return sd;
    }
    
    public static double [] calcMetrics(double [] delta, double [] theta, double [] alpha, double [] beta) {

        
    	double delta_mean = getMean(delta);
        double theta_mean = getMean(theta);
        double alpha_mean = getMean(alpha);
        double beta_mean = getMean(beta);

        double delta_stdev = getStdev(delta, delta_mean);
        double theta_stdev = getStdev(theta, theta_mean);
        double alpha_stdev = getStdev(alpha, alpha_mean);
        double beta_stdev = getStdev(beta, beta_mean);

        double alphaBytheta = alpha_mean / theta_mean;
        double alphaBydelta = alpha_mean / delta_mean;
        double thetaBydelta = theta_mean / delta_mean;
        double betaBydelta = beta_mean / delta_mean;

        double [] result = {delta_mean, delta_stdev, theta_mean, theta_stdev,
                             alpha_mean, alpha_stdev, beta_mean, beta_stdev,
                             alphaBytheta, alphaBydelta, thetaBydelta, betaBydelta};
        return result;
    }
}


