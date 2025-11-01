# 🧪 UNIT TESTS GUIDE

## 📋 Tổng quan

Dự án sử dụng **JUnit 5** và **Mockito** để viết unit tests cho các service classes.

---

## 🎯 Test Coverage

### ✅ Đã có tests:

1. **AuthServiceTest** - Authentication & User Management
   - ✅ User registration
   - ✅ User login
   - ✅ Password validation
   - ✅ Disabled user handling
   - ✅ Logout all devices

2. **WalletServiceTest** - Wallet Management
   - ✅ List user wallets
   - ✅ Create wallet
   - ✅ Delete wallet
   - ✅ Error handling

3. **TransactionServiceTest** - Transaction Management
   - ✅ Create income transaction
   - ✅ Create expense transaction
   - ✅ Wallet balance updates
   - ✅ Category type validation
   - ✅ Delete transaction (balance revert)

---

## 🚀 Running Tests

### Run all tests
```bash
mvn test
```

### Run specific test class
```bash
mvn test -Dtest=AuthServiceTest
```

### Run specific test method
```bash
mvn test -Dtest=AuthServiceTest#register_WithValidData_ShouldCreateUser
```

### Run with coverage report
```bash
mvn test jacoco:report
```
Report will be in: `target/site/jacoco/index.html`

---

## 📝 Writing New Tests

### Test Structure (AAA Pattern)

```java
@Test
void methodName_Condition_ExpectedBehavior() {
    // Arrange - Setup test data and mocks
    Long userId = 1L;
    WalletDto dto = new WalletDto();
    dto.setName("Test Wallet");
    
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    
    // Act - Execute the method being tested
    WalletDto result = walletService.create(dto, userId);
    
    // Assert - Verify the results
    assertNotNull(result);
    assertEquals("Test Wallet", result.getName());
    verify(walletRepository, times(1)).save(any(Wallet.class));
}
```

### Naming Convention

```
methodName_Condition_ExpectedBehavior
```

Examples:
- `create_WithValidData_ShouldCreateWallet`
- `delete_WithNonExistentWallet_ShouldThrowException`
- `login_WithInvalidPassword_ShouldThrowException`

---

## 🛠️ Test Utilities

### Mockito Annotations

```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    
    @Mock  // Create a mock instance
    private MyRepository repository;
    
    @InjectMocks  // Inject mocks into this instance
    private MyService service;
    
    @BeforeEach
    void setUp() {
        // Setup code runs before each test
    }
}
```

### Common Mockito Methods

```java
// When-Then
when(repository.findById(1L)).thenReturn(Optional.of(entity));

// Verify
verify(repository, times(1)).save(any(Entity.class));
verify(repository, never()).delete(any());

// Argument Captors
ArgumentCaptor<Entity> captor = ArgumentCaptor.forClass(Entity.class);
verify(repository).save(captor.capture());
Entity savedEntity = captor.getValue();
```

### JUnit Assertions

```java
// Basic assertions
assertNotNull(result);
assertEquals(expected, actual);
assertTrue(condition);
assertFalse(condition);

// Exception assertions
assertThrows(IllegalArgumentException.class, () -> {
    service.methodThatThrows();
});

// Collection assertions
assertEquals(2, list.size());
assertTrue(list.contains(item));
```

---

## 📊 TODO: Expand Test Coverage

### High Priority
- [ ] **BudgetService** tests (create, alerts, budget tracking)
- [ ] **CategoryService** tests (CRUD operations)
- [ ] **EmailService** tests (mock email sending)

### Medium Priority
- [ ] **NotificationService** tests (notifications, scheduled jobs)
- [ ] **RecurringTransactionService** tests (automation logic)
- [ ] **FinancialGoalService** tests (progress tracking)

### Low Priority
- [ ] **ReportService** tests (analytics calculations)
- [ ] **ExportService** tests (Excel/PDF generation)
- [ ] **FileStorageService** tests (file upload/download)

---

## 🎯 Test Best Practices

### ✅ DO:
1. **Test one thing per test** - Each test should verify one behavior
2. **Use descriptive names** - Test name should explain what it tests
3. **Follow AAA pattern** - Arrange, Act, Assert
4. **Mock external dependencies** - Don't hit real database/external APIs
5. **Test edge cases** - Null values, empty strings, negative numbers
6. **Test error scenarios** - What happens when things go wrong?

### ❌ DON'T:
1. **Don't test getters/setters** - Unless they have logic
2. **Don't test Spring framework** - Trust that Spring works
3. **Don't test database queries** - That's integration test territory
4. **Don't use real database** - Use mocks or in-memory DB for integration tests
5. **Don't skip test cleanup** - Use @BeforeEach and @AfterEach properly

---

## 🔧 Integration Tests (TODO)

### Setup

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional  // Rollback after each test
class TransactionControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Test
    void createTransaction_ShouldReturnCreatedTransaction() throws Exception {
        // Test with real Spring context
        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{...json...}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }
}
```

---

## 📈 Coverage Goals

| Component | Target Coverage | Current |
|-----------|----------------|---------|
| Services | 80% | ~30% |
| Controllers | 70% | 0% |
| Utilities | 90% | 0% |
| Overall | 75% | ~15% |

---

## 🚦 CI/CD Integration

### GitHub Actions (TODO)

```yaml
name: Java CI

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    - name: Set up JDK 17
      uses: actions/setup-java@v2
      with:
        java-version: '17'
    - name: Run tests
      run: mvn test
    - name: Generate coverage report
      run: mvn jacoco:report
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v2
```

---

## 📚 Resources

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [Test-Driven Development](https://martinfowler.com/bliki/TestDrivenDevelopment.html)

---

**Last Updated:** 2025-11-01  
**Maintainer:** Finance App Team

