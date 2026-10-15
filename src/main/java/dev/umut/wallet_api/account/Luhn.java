package dev.umut.wallet_api.account;

final class Luhn {

    private Luhn() {
    }

    static int checkDigit(String body) {
        int sum = 0;
        boolean doubleIt = true;
        for (int i = body.length() - 1; i >= 0; i--) {
            int digit = body.charAt(i) - '0';
            if (doubleIt) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleIt = !doubleIt;
        }
        return (10 - sum % 10) % 10;
    }

    static boolean isValid(String number){

        if(number==null || !number.matches("\\d{2,}")){
            return false;
        }
        String body=number.substring(0,number.length()-1);
        int check=number.charAt(number.length()-1)-'0';
        return check==checkDigit(body);
    }
}
