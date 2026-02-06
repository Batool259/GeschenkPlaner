package model;

public class GiftItem {

    public String id;
    public String title;
    public Double price;
    public boolean bought;

    public GiftItem() {}

    public GiftItem(String id, String title, Double price, boolean bought) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.bought = bought;
    }
}
