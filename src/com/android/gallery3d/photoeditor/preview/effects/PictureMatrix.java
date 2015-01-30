package com.android.gallery3d.photoeditor.preview.effects;

/**
* @author Saman Alvi
*
*/
public class PictureMatrix {

	    private int matrixSize;
	    public double[][] matrix;
	    public int factor = 0, offset = 0;

	    public PictureMatrix() {
	        this(3);
	    }

	    public PictureMatrix(int size) {
	        matrixSize = size;
	        matrix = new double[matrixSize][matrixSize];
	    }

	    public void setValues(double value) {
	        for (int i = 0; i < matrixSize; i++) {
	            for (int j = 0; j < matrixSize; j++) {
	                matrix[i][j] = value;
	            }
	        }
	    }

	    public void setMiddle(int value) {

	        matrix[(matrixSize / 2)][(matrixSize / 2)] = value;

	        //matrix [1][1] = value;
	        //factor = value + ((matrixSize * matrixSize) - 1);

	        for (int i = 0; i < matrixSize; i++) {
	            for (int j = 0; j < matrixSize; j++) {
	                factor += matrix[i][j];
	            }
	        }

	        factor = value + 8;
	    }

	    public void setFactor(int value) {
	        factor = value;
	    }

	    public int getMatrixSize() {
	        return matrixSize;
	    }

	    public void setGaussian() {

	        /*
	         * assuming a 3x3 matrix to look like the following:
	         *    1   2   1
	         *    2   4   2
	         *    1   2   1
	         */
	        for (int i = 0; i < matrixSize; i++) {
	            for (int j = 0; j < matrixSize; j++) {

	                //center square
	                if ((i % 2 != 0) && (j % 2 != 0)) {
	                    matrix[i][j] = 4;
	                } else if ((i % 2 != 0) && (j % 2 == 0)) {
	                    matrix[i][j] = 2;
	                } else if ((i % 2 == 0) && (j % 2 != 0)) {
	                    matrix[i][j] = 2;
	                } else {
	                    matrix[i][j] = 1;
	                }

	                factor += matrix[i][j];
	                offset = 0;
	            }
	        }

	    }
}
