package main;

import java.util.Random;

public class Test {

	public static void main(String[] args) {
		Random random = new Random();
		int upperBound = 1000;
		for (int i = 0; i < 100; i++) {
			int r = random.nextInt(upperBound);
			int teller = 0;
			System.out.print(r + ": ");
			if (r < (teller += 1)) {
				System.out.println(1);
			} else if (r < (teller += 2)) {
				System.out.println(2);
			} else if (r < (teller += 3)) {
				System.out.println(3);
			} else if (r < (teller += 3)) {
				System.out.println(4);
			} else if (r < (teller += 2)) {
				System.out.println(5);
			} else if (r < (teller += 1)) {
				System.out.println(6);
			} else {
				upperBound = teller;
				System.out.println("reset upperbound");
			}
		}
	}

}
