package banking;

import java.sql.*;
import java.util.*;

public class Main {
    private static final String optionsUnauthorized = "\n1. Create an account\n2. Log into account\n0. Exit";
    private static final String optionsAuthorized = "\n1. Balance\n2. Log out\n0. Exit";
    private static final Map<Long, Account> accounts = new HashMap<>();
    private static final Random random = new Random();
    private static final int bankIdentificationNumber = 400000;
    private static final Scanner scanner = new Scanner(System.in);
    private static boolean authorized = false;
    private static Account currentUser = null;

    public static void main(String[] args) {

        if (args.length != 2 || !"-fileName".equals(args[0])) {
            System.out.println("\nPlease specify the database file name as a command line argument -fileName");
            System.exit(0);
        }

        String url = "jdbc:sqlite:" + args[1];

        Connection connection = null;

        try {
            connection = DriverManager.getConnection(url);
        } catch (SQLException exception) {
            exception.printStackTrace();
        } finally {
            if (connection == null) {
                System.out.println("\nConnection failed!");
                System.exit(0);
            }
        }

        String sql = "CREATE TABLE IF NOT EXISTS card ("
                + "id INTEGER, "
                + "number TEXT, "
                + "pin TEXT, "
                + "balance INTEGER DEFAULT 0"
                + ");";

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException exception) {
            exception.printStackTrace();
            System.exit(0);
        }

        System.out.println(optionsUnauthorized);

        int input;

        while (true) {
            try {
                input = scanner.nextInt();
                break;
            } catch (InputMismatchException inputMismatchException) {
                System.out.println("Invalid input");
            }
        }

        boolean running = true;

        while (running) {
            switch (input) {
                case 1:
                    if (authorized) {
                        getBalance();
                    } else {
                        createAccount(connection);
                    }

                    break;
                case 2:
                    if (authorized) {
                        logOut();
                    } else {
                        logIntoAccount(connection);
                    }

                    break;
                case 0:
                    System.out.println("\nBye!");
                    running = false;
                    break;
                default:
                    System.out.println("\nInvalid input");
                    break;
            }

            if (running) {
                if (authorized) {
                    System.out.println(optionsAuthorized);
                } else {
                    System.out.println(optionsUnauthorized);
                }

                while (true) {
                    try {
                        input = scanner.nextInt();
                        break;
                    } catch (InputMismatchException inputMismatchException) {
                        System.out.println("\nInvalid input");
                    }
                }
            }
        }
    }

    private static void createAccount(Connection connection) {
        int accountIdentifier = random.nextInt(1000000000);
        long num = (long) bankIdentificationNumber * 1000000000L + (long) accountIdentifier;
        long cardNumber = num * 10L + getChecksum(num);

        int pinNum = random.nextInt(10000);
        String pin = String.format("%1$4s", pinNum).replace(' ', '0');

        // Account newAccount = new Account(cardNumber, pin.hashCode());
        Account newAccount = new Account(cardNumber, pin); // Unsafe temporary measure

        if (accounts.putIfAbsent(cardNumber, newAccount) == null) {

            String sql = "INSERT INTO card(id, number, pin, balance) VALUES ("
                    + newAccount.getId() + ", '"
                    + newAccount.getCardNumber() + "', '"
                    + newAccount.getPin() + "', "
                    + newAccount.getBalance()
                    + ");";

            try (Statement statement = connection.createStatement()) {
                if (statement.executeUpdate(sql) == 1) {
                    System.out.println("\nYour card has been created\nYour card number:\n" + cardNumber + "\nYour card PIN:\n" + pin);
                } else {
                    System.out.println("\nFailed to update the database");
                }

            } catch (SQLException exception) {
                exception.printStackTrace();
            }

        } else {
            System.out.println("\nThere has been an extremely improbable error!\n" +
                    "You're probably the first and the last person to ever see this message, so just try again.");
        }
    }

    // Utilizes the Luhn Algorithm
    private static int getChecksum(long num) {
        int[] digits = new int[15];

        int sum = 0;

        for (int i = 0; i < digits.length; i++) {
            digits[i] = (int) Math.floor(num % 10L);
            num = num / 10L;

            if ((i + 1) % 2 != 0) {
                digits[i] *= 2;
            }

            if (digits[i] > 9) {
                digits[i] -= 9;
            }

            sum += digits[i];
        }

        return (int) Math.ceil(sum / 10f) * 10 - sum;
    }

    private static boolean checkCardNumber(long num) {
        int checkSum = (int) Math.floor(num % 10L);
        num = num / 10L;

        return getChecksum(num) == checkSum;
    }

    private static void logIntoAccount(Connection connection) {
        System.out.println("\nEnter your card number:");
        long cardNumber = scanner.nextLong();
        System.out.println("\nEnter your PIN:");
        String pin = scanner.next();

        if (!checkCardNumber(cardNumber)) {
            System.out.println("\nWrong card number or PIN!");
            return;
        }

        String sql = "SELECT id, number, pin, balance FROM card WHERE number = " + cardNumber;

        Account currentAccount = accounts.get(cardNumber);

        if (currentAccount == null) {
            currentAccount = new Account();
        }

        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(sql)) {
                resultSet.next();

                currentAccount.setId(resultSet.getInt("id"));
                currentAccount.setCardNumber(Long.parseLong(resultSet.getString("number")));
                currentAccount.setPin(resultSet.getString("pin"));
                currentAccount.setBalance(resultSet.getInt("balance"));
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        if (currentAccount.getPin().equals(pin)) {
            currentUser = currentAccount;
            authorized = true;
            System.out.println("\nYou have successfully logged in!");
        } else {
            System.out.println("\nWrong card number or PIN!");
        }

    }

    private static void getBalance() {
        if (currentUser != null) {
            System.out.println("\nBalance: " + currentUser.getBalance());
        } else {
            System.out.println("\nAuthorize first.");
        }
    }

    private static void logOut() {
        authorized = false;
        currentUser = null;

        System.out.println("\nYou have successfully logged out!");
    }
}