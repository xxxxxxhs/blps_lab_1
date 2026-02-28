package ru.blps.lab_1.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomOrderDataGenerator {

    private static final List<String> CITIES = List.of(
        "Москва", "Санкт-Петербург", "Казань", "Новосибирск", "Екатеринбург"
    );

    private static final List<String> STREETS = List.of(
        "Ленина", "Пушкина", "Гагарина", "Советская", "Мира", "Центральная"
    );

    private static final List<String> DISH_NAMES = List.of(
        "Пицца Маргарита", "Бургер", "Кофе латте", "Салат Цезарь", "Суши сет",
        "Пепперони", "Картофель фри", "Кола", "Шаурма", "Плов"
    );

    private static final List<String> COMMENTS = List.of(
        "", "Не звонить", "Оставить у двери", "Позвонить за 5 минут"
    );

    private RandomOrderDataGenerator() {
    }

    public static Long randomClientId() {
        return ThreadLocalRandom.current().nextLong(1, 10000);
    }

    public static Long randomRestaurantId() {
        return ThreadLocalRandom.current().nextLong(1, 500);
    }

    public static String randomCity() {
        return CITIES.get(ThreadLocalRandom.current().nextInt(CITIES.size()));
    }

    public static String randomRestaurantAddress() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        String street = STREETS.get(r.nextInt(STREETS.size()));
        int house = r.nextInt(1, 150);
        return "ул. " + street + ", " + house;
    }

    public static String randomDeliveryAddress() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        String street = STREETS.get(r.nextInt(STREETS.size()));
        int house = r.nextInt(1, 150);
        int apartment = r.nextInt(1, 100);
        return "ул. " + street + ", " + house + ", кв. " + apartment;
    }

    public static String randomPhone() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return "79" + String.format("%09d", r.nextLong(0, 1000000000));
    }

    public static String randomComment() {
        return COMMENTS.get(ThreadLocalRandom.current().nextInt(COMMENTS.size()));
    }

    public static List<Item> randomItems() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int itemCount = r.nextInt(2, 5);
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            String name = DISH_NAMES.get(r.nextInt(DISH_NAMES.size()));
            int quantity = r.nextInt(1, 4);
            double price = 100 + r.nextInt(50, 1501);
            items.add(new Item(name, quantity, price));
        }
        return items;
    }

    public static class Item {
        private final String name;
        private final int quantity;
        private final double price;

        public Item(String name, int quantity, double price) {
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }

        public String getName() {
            return name;
        }

        public int getQuantity() {
            return quantity;
        }

        public double getPrice() {
            return price;
        }
    }
}
