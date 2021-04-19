package banking;

public class Account {
    private static long idSeq = 0;
    private long id;
    private long cardNumber;
    private int pinHash;
    private long balance;

    public Account() {
    }

    public Account(long cardNumber, int pinHash) {
        id = getNextId();
        this.cardNumber = cardNumber;
        this.pinHash = pinHash;
        balance = 0;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setCardNumber(long cardNumber) {
        this.cardNumber = cardNumber;
    }

        public void setPinHash(int pinHash) {
        this.pinHash = pinHash;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public long getId() {
        return id;
    }

    public long getCardNumber() {
        return cardNumber;
    }

    public int getPinHash() {
        return pinHash;
    }

    public long getBalance() {
        return balance;
    }

    public long withdrawSum(long sum) {
        if (sum > 0) {
            balance -= sum;
            return sum;
        }

        return 0;
    }

    public void depositSum(long sum) {
        if (sum > 0) {
            balance += sum;
        }
    }

    private static long getNextId() {
        return idSeq++;
    }
}
