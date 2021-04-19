package banking;

import java.sql.*;
import java.util.*;

public class Main {
    private static final String optionsUnauthorized = "\n1. Create an account\n2. Log into account\n0. Exit";
    private static final String optionsAuthorized = "\n1. Balance\n2. Add income\n3. Do transfer\n4. Close account\n5. Log out\n0. Exit";
    private static final Random random = new Random();
    private static final int bankIdentificationNumber = 400000;
    private static final Scanner scanner = new Scanner(System.in);
    private static boolean authorized = false;
    private static Account currentUser = null;

    public static void main(String[] args) {

        if (args.length < 2 || !"-fileName".equals(args[0])) {
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

        try {
            if (connection.getAutoCommit()) {
                connection.setAutoCommit(false);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        String sql = "CREATE TABLE IF NOT EXISTS card ("
                + "id INTEGER, "
                + "number INTEGER, "
                + "pin_hash INTEGER, "
                + "balance INTEGER DEFAULT 0"
                + ");";

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
            connection.commit();
        } catch (SQLException exception) {
            exception.printStackTrace();
            System.out.println("\nTable creation failed!");
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
                        addIncome(connection);
                    } else {
                        logIntoAccount(connection);
                    }

                    break;
                case 3:
                    if (authorized) {
                        doTransfer(connection);
                    }

                    break;
                case 4:
                    if (authorized) {
                        closeCurrentAccount(connection);
                    }

                    break;
                case 5:
                    if (authorized) {
                        logOut();
                    }

                    break;
                case 0:
                    try {
                        connection.close();
                        System.out.println("\nBye!");
                        running = false;
                    } catch (SQLException exception) {
                        exception.printStackTrace();
                        System.out.println("\nCan't close connection!");
                    }
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

         Account newAccount = new Account(cardNumber, pin.hashCode());

        String sql = "INSERT INTO card(id, number, pin_hash, balance) VALUES (?, ?, ?, ?);";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, newAccount.getId());
            statement.setLong(2, newAccount.getCardNumber());
            statement.setInt(3, newAccount.getPinHash());
            statement.setLong(4, newAccount.getBalance());

            statement.executeUpdate();

            System.out.println("\nYour card has been created\nYour card number:\n" + cardNumber + "\nYour card PIN:\n" + pin);
            connection.commit();
        } catch (SQLException exception) {
            System.out.println("\nFailed to create an account");
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

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean checkCardNumber(long num) {
        int checkSum = (int) Math.floor(num % 10L);
        num = num / 10L;

        return getChecksum(num) == checkSum;
    }

    private static void logIntoAccount(Connection connection) {
        System.out.println("\nEnter your card number:");
        long cardNumber = scanner.nextLong();
        System.out.println("Enter your PIN:");
        String pin = scanner.next();

        if (!checkCardNumber(cardNumber)) {
            System.out.println("\nWrong card number or PIN!");
            return;
        }

        Account selectedAccount = retrieveAccountByCardNumber(connection, cardNumber);

        if (selectedAccount != null && selectedAccount.getPinHash() == pin.hashCode()) {
            currentUser = selectedAccount;
            authorized = true;
            System.out.println("\nYou have successfully logged in!");
        } else {
            System.out.println("\nWrong card number or PIN!");
        }

    }

    private static void addIncome(Connection connection) {
        System.out.println("\nEnter income:");
        long income = scanner.nextLong();

        currentUser.depositSum(income);

        String sql = "UPDATE card SET balance = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, currentUser.getBalance());
            statement.setLong(2, currentUser.getId());
            statement.executeUpdate();

            System.out.println("\nIncome was added!");
            connection.commit();
        } catch (SQLException exception) {
            exception.printStackTrace();
            System.out.println("\nFailed to add income");
        }
    }

    private static void doTransfer(Connection connection) {
        System.out.println("\nTransfer");
        System.out.println("Enter card number:");
        long cardNumber = scanner.nextLong();

        if (!checkCardNumber(cardNumber)) {
            System.out.println("Probably you made a mistake in the card number. Please try again!");
            return;
        }

        if (cardNumber == currentUser.getCardNumber()) {
            System.out.println("You can't transfer money to the same account");
            return;
        }

        Account receiverAccount = retrieveAccountByCardNumber(connection, cardNumber);

        if (receiverAccount == null) {
            System.out.println("Such a card does not exist.");
            return;
        }

        System.out.println("Enter how much money you want to transfer:");
        long sum = scanner.nextLong();

        if (currentUser.getBalance() < sum) {
            System.out.println("Not enough money!");
            return;
        }

        receiverAccount.depositSum(currentUser.withdrawSum(sum));

        String sql = "UPDATE card SET balance = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, currentUser.getBalance());
            statement.setLong(2, currentUser.getId());
            statement.executeUpdate();

            statement.setLong(1, receiverAccount.getBalance());
            statement.setLong(2, receiverAccount.getId());
            statement.executeUpdate();

            System.out.println("\nSuccess!");
            connection.commit();
        } catch (SQLException exception) {
            System.out.println("\nFailed to transfer");
            try {
                connection.rollback();
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
        }
    }

    private static Account retrieveAccountByCardNumber(Connection connection, long cardNumber) {
        String sql = "SELECT id, number, pin_hash, balance FROM card WHERE number = ?";

        Account selectedAccount = new Account();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, String.valueOf(cardNumber));

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                selectedAccount.setId(resultSet.getLong("id"));
                selectedAccount.setCardNumber(resultSet.getLong("number"));
                selectedAccount.setPinHash(resultSet.getInt("pin_hash"));
                selectedAccount.setBalance(resultSet.getLong("balance"));
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return null;
        }

        return selectedAccount;
    }

    private static void closeCurrentAccount(Connection connection) {
        String sql = "DELETE FROM card WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, currentUser.getId());
            statement.executeUpdate();

            System.out.println("\nThe account has been closed!");
            connection.commit();
            currentUser = null;
            authorized = false;
        } catch (SQLException exception) {
            System.out.println("\nFailed to close the account");
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