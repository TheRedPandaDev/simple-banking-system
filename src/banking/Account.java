package banking;

public class Account {
    private static long idSeq = 0;
    private long id;
    private long cardNumber;
    private String pin; // Unsafe temporary measure
    // private int pinHash;
    private long balance;

    public Account() {
    }

    public Account(long cardNumber, String pin) {
        id = getNextId();
        this.cardNumber = cardNumber;
        this.pin = pin;
        // this.pinHash = pinHash;
        balance = 0;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setCardNumber(long cardNumber) {
        this.cardNumber = cardNumber;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    //    public void setPinHash(int pinHash) {
//        this.pinHash = pinHash;
//    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public long getId() {
        return id;
    }

    public long getCardNumber() {
        return cardNumber;
    }

    public String getPin() {
        return pin;
    }

    //    public int getPinHash() {
//        return pinHash;
//    }

    public long getBalance() {
        return balance;
    }

    public void changeBalance(long sum) {
        balance += sum;
    }

    private static long getNextId() {
        return idSeq++;
    }
}
