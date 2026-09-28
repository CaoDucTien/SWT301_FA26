# BÃO CÃO MUTATION TESTING THá»¦ CÃ”NG & JACOCO COVERAGE

## 1. Káº¿t quáº£ Ä‘o JaCoCo Coverage
- **Class AccountValidator:** Line Coverage: 100%, Branch Coverage: 92%
- **Class AccountService:** Line Coverage: 95%, Branch Coverage: 88%
- **TiÃªu chuáº©n Ä‘áº¡t Ä‘Æ°á»£c:** VÆ°á»£t má»©c yÃªu cáº§u (Line >= 80%, Branch >= 70%).

---

## 2. Báº£ng 3 Lá»—i Giáº£ Láº­p Thá»§ CÃ´ng (Manual Mutation Testing)

| # | Mutant | Vá»‹ trÃ­ sá»­a trong Production Code | Test Case báº¯t lá»—i (Killed By) | Tráº¡ng thÃ¡i |
|---|---|---|---|---|
| **M1** | Äá»•i toÃ¡n tá»­ Ä‘iá»u kiá»‡n khÃ³a tÃ i khoáº£n | Trong AccountService.login(): sá»­a >= MAX_FAILED_ATTEMPTS thÃ nh > MAX_FAILED_ATTEMPTS | Login.login_WrongPassword5thTime_LocksAccount | **KILLED** (BÃ¡o FAIL vÃ¬ láº§n thá»© 5 chÆ°a khÃ³a tÃ i khoáº£n) |
| **M2** | Bá» qua viá»‡c reset bá»™ Ä‘áº¿m khi Ä‘Äƒng nháº­p Ä‘Ãºng | Trong AccountService.login(): xÃ³a dÃ²ng cc.resetFailedAttempts(); | Login.login_CorrectCredentials_ReturnsSuccessAndResetsCounter | **KILLED** (BÃ¡o FAIL vÃ¬ failedAttempts khÃ´ng vá» 0 sau khi Ä‘Äƒng nháº­p thÃ nh cÃ´ng) |
| **M3** | Sá»­a biÃªn Ä‘á»™ dÃ i username | Trong AccountValidator.isValidUsername(): sá»­a regex {4,19} thÃ nh {4,20} | Username.isValidUsername_BoundaryLength (Ä‘á»™ dÃ i 21) | **KILLED** (BÃ¡o FAIL vÃ¬ username 21 kÃ½ tá»± tráº£ vá» true thay vÃ¬ false) |

> Cáº£ 3 mutant Ä‘á»u Ä‘Ã£ Ä‘Æ°á»£c hoÃ n tÃ¡c vá» tráº¡ng thÃ¡i ban Ä‘áº§u, Ä‘áº£m báº£o toÃ n bá»™ test suite cháº¡y xanh 100%.