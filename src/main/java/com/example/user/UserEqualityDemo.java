package com.example.user;

public final class UserEqualityDemo {

    public static void main(String[] args) {
        User user1 = new User("Emmanuel", 30);
        User user2 = new User("Emmanuel", 30);

        System.out.println(user1.equals(user2));
        System.out.println(user1.hashCode());
        System.out.println(user2.hashCode());
    }

    private UserEqualityDemo() { /* entry point only */ }
}
