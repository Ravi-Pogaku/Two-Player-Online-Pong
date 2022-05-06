module com.example.regularpong {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.regularpong to javafx.fxml;
    exports com.example.regularpong;
}