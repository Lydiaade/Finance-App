package app;

import controller.FinanceManagerController;

@SpringBootApplication
public class app {
    public static void main(String[] args) {
        FinanceManagerController controller = new FinanceManagerController();
        controller.run();
    }
}
