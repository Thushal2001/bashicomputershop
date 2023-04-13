package lk.ijse.computershop.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import lk.ijse.computershop.dto.Order;
import lk.ijse.computershop.dto.Customer;
import lk.ijse.computershop.dto.Item;
import lk.ijse.computershop.dto.tm.OrderTM;
import lk.ijse.computershop.model.CustomerModel;
import lk.ijse.computershop.model.ItemModel;
import lk.ijse.computershop.model.OrderModel;
import lk.ijse.computershop.model.PlaceOrderModel;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ManageordersFormController implements Initializable {

    @FXML
    private TextField txtOrderId;
    @FXML
    private TextField txtOrderDate;
    @FXML
    private ComboBox<String> cmbCustomerId;
    @FXML
    private TextField txtCustomerName;
    @FXML
    private ComboBox<String> cmbItemCode;
    @FXML
    private TextField txtDescription;
    @FXML
    private TextField txtQtyOnHand;
    @FXML
    private TextField txtUnitPrice;
    @FXML
    private TextField txtQty;
    @FXML
    private TextField txtNetTotal;
    @FXML
    private TableView tblOrderCart;
    @FXML
    private TableColumn colCode;
    @FXML
    private TableColumn colDescription;
    @FXML
    private TableColumn colQty;
    @FXML
    private TableColumn colUnitPrice;
    @FXML
    private TableColumn colTotal;
    @FXML
    private TableColumn colAction;

    private ObservableList<OrderTM> observableList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setOrderDate();
        generateNextOrderId();
        setCellValueFactory();
        loadCustomerIds();
        loadItemCodes();
    }

    private void setOrderDate() {
        txtOrderDate.setText(String.valueOf(LocalDate.now()));
    }

    private void generateNextOrderId() {
        try {
            String id = OrderModel.getNextOrderId();
            txtOrderId.setText(id);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "please try again...!").show();
        }
    }

    private void setCellValueFactory() {
        colCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("qty"));
        colUnitPrice.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colAction.setCellValueFactory(new PropertyValueFactory<>("btn"));
    }

    private void loadCustomerIds() {
        try {
            ObservableList<String> observableList = FXCollections.observableArrayList();
            List<String> customerid = CustomerModel.loadIds();

            for (String id : customerid) {
                observableList.add(id);
            }
            cmbCustomerId.setItems(observableList);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "please try again...!").show();
        }
    }

    private void loadItemCodes() {
        try {
            ObservableList<String> observableList = FXCollections.observableArrayList();
            List<String> itemcodes = ItemModel.loadCodes();

            for (String code : itemcodes) {
                observableList.add(code);
            }
            cmbItemCode.setItems(observableList);

        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "please try again...!").show();
        }
    }

    @FXML
    private void cmbCustomerIdOnAction(ActionEvent event) {
        String id = cmbCustomerId.getValue();

        try {
            Customer customer = CustomerModel.searchById(id);
            txtCustomerName.setText(customer.getName());
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "please try again...!").show();
        }
    }

    @FXML
    private void cmbItemCodeOnAction(ActionEvent event) {
        String code = cmbItemCode.getValue();

        try {
            Item item = ItemModel.searchById(code);
            fillItemFields(item);

            txtQty.requestFocus();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "please try again...!").show();
        }
    }

    private void fillItemFields(Item item) {
        txtDescription.setText(item.getDescription());
        txtUnitPrice.setText(String.valueOf(item.getUnitprice()));
        txtQtyOnHand.setText(String.valueOf(item.getQtyonhand()));
    }

    @FXML
    private void addToCartOnAction(ActionEvent event) {
        try {
            String code = cmbItemCode.getValue();
            String description = txtDescription.getText();
            int qty = Integer.parseInt(txtQty.getText());
            double unitPrice = Double.parseDouble(txtUnitPrice.getText());

            double total = qty * unitPrice;
            Button btnRemove = new Button("Remove");
            btnRemove.setCursor(Cursor.HAND);

            setRemoveBtnOnAction(btnRemove);    //set action to the btnRemove

            if (!observableList.isEmpty()) {
                for (int i = 0; i < tblOrderCart.getItems().size(); i++) {
                    if (colCode.getCellData(i).equals(code)) {
                        qty += (int) colQty.getCellData(i);
                        total = qty * unitPrice;

                        observableList.get(i).setQty(qty);
                        observableList.get(i).setTotal(total);

                        tblOrderCart.refresh();
                        calculateNetTotal();
                        return;
                    }
                }
            }

            OrderTM tm = new OrderTM(code, description, qty, unitPrice, total, btnRemove);

            observableList.add(tm);
            tblOrderCart.setItems(observableList);
            calculateNetTotal();

            txtQty.setText("");
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "please try again...!").show();
        }
    }

    private void setRemoveBtnOnAction(Button btn) {
        btn.setOnAction((e) -> {
            ButtonType yes = new ButtonType("Yes", ButtonBar.ButtonData.OK_DONE);
            ButtonType no = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);

            Optional<ButtonType> result = new Alert(Alert.AlertType.INFORMATION, "Are you sure to remove?", yes, no).showAndWait();

            if (result.orElse(no) == yes) {

                int index = tblOrderCart.getSelectionModel().getSelectedIndex();
                observableList.remove(index+1);

                tblOrderCart.refresh();
                calculateNetTotal();
            }

        });
    }

    private void calculateNetTotal() {
        double netTotal = 0.0;
        for (int i = 0; i < tblOrderCart.getItems().size(); i++) {
            double total = (double) colTotal.getCellData(i);
            netTotal += total;
        }
        txtNetTotal.setText(String.valueOf(netTotal));
    }

    @FXML
    private void placeOrderOnAction(ActionEvent event) {
        String oId = txtOrderId.getText();
        String cusId = cmbCustomerId.getValue();

        List<Order> orderList = new ArrayList<>();

        for (int i = 0; i < tblOrderCart.getItems().size(); i++) {
            OrderTM orderTM = observableList.get(i);

            Order dto = new Order(
                    orderTM.getCode(),
                    orderTM.getQty(),
                    orderTM.getUnitPrice()
            );
            orderList.add(dto);
        }

        boolean isPlaced = false;
        try {
            isPlaced = PlaceOrderModel.placeOrder(oId, cusId, orderList);
            if (isPlaced) {
                new Alert(Alert.AlertType.INFORMATION, "Order Placed...!").show();
            } else {
                new Alert(Alert.AlertType.ERROR, "Order is Not Placed...!").show();
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "please try again...!r").show();
        }
    }
}
