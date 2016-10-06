import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class Main {
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		//System.out.println("Hello World.");
		LDAFileReader LDA = new LDAFileReader(args);
		LDA.GibbsSampling();
		LDA.WriteRes();
		
	}	

}
