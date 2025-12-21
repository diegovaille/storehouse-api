# Parâmetros de Cálculo - Tipos de Valores

## ⚠️ Importante: Valores Decimais vs Inteiros

O campo `valor` nos parâmetros de cálculo é do tipo **`DOUBLE PRECISION`** no banco de dados, o que significa que aceita **TANTO valores decimais quanto inteiros**.

### 📋 Parâmetros Existentes

| Chave | Valor Padrão | Tipo Real | Descrição |
|-------|--------------|-----------|-----------|
| `UNIDADES_POR_EMBALAGEM_KG` | 700 | Inteiro | Quantidade de unidades por KG de embalagem |
| `TOTAL_PACOTES_POR_CAIXA` | 120 | Inteiro | Total de pacotes que cabem em uma caixa |
| `TOTAL_UNIDADE_POR_KG` | 266 | Inteiro | Total de unidades produzidas por KG de matéria-prima |
| `TOTAL_UNIDADE_ACUCAR_POR_KG` | 222 | Inteiro | Total de unidades por KG de açúcar (Coco, Maçã Verde) |
| **`UNIDADES_POR_PACOTE`** | **4.4** | **Decimal** ✨ | Quantidade de unidades por pacote |

### 💡 Observações

- **Maioria:** Valores inteiros (700, 120, 266, 222)
- **Exceção:** `UNIDADES_POR_PACOTE = 4.4` é decimal
- **Flexibilidade:** O sistema aceita qualquer valor decimal ou inteiro

### 🔧 Exemplos de Uso na API

#### Atualizar com valor inteiro:
```json
PUT /api/pinguimice-admin/parametros-calculo/TOTAL_UNIDADE_POR_KG
{
  "chave": "TOTAL_UNIDADE_POR_KG",
  "valor": 280,
  "descricao": "Atualizado para nova máquina"
}
```

#### Atualizar com valor decimal:
```json
PUT /api/pinguimice-admin/parametros-calculo/UNIDADES_POR_PACOTE
{
  "chave": "UNIDADES_POR_PACOTE",
  "valor": 4.5,
  "descricao": "Novo padrão de empacotamento"
}
```

### 📊 Tipo de Dados

**Backend (Kotlin):**
```kotlin
@Column(nullable = false)
var valor: Double  // Aceita 4.4 ou 266
```

**Banco de Dados (PostgreSQL):**
```sql
valor DOUBLE PRECISION NOT NULL  -- Aceita decimais e inteiros
```

**OpenAPI Spec:**
```yaml
valor:
  type: number
  format: double
  example: 4.4  # Exemplo com decimal
  description: Valor numérico (aceita decimais como 4.4 ou inteiros como 266)
```

### ✅ Validações Recomendadas

Embora o tipo permita qualquer número, é recomendado validar no frontend:

- **Valores positivos:** Não aceitar negativos
- **Ranges razoáveis:** Ex: 1-10000 para evitar erros de digitação
- **Precisão:** Limitar a 2 casas decimais na UI (4.44 → 4.44, mas internamente pode ter mais)

### 🎨 Sugestão de UI (Frontend)

```html
<!-- Input que aceita decimais -->
<input 
  type="number" 
  step="0.1"  <!-- Permite decimais -->
  min="0" 
  value="4.4"
  placeholder="Ex: 4.4 ou 266"
/>

<small class="hint">
  💡 Aceita valores decimais (ex: 4.4) ou inteiros (ex: 266)
</small>
```

### 📝 Exemplos Práticos

**Cenário 1:** Ajustar valor decimal
```bash
# Antes: UNIDADES_POR_PACOTE = 4.4
# Depois: UNIDADES_POR_PACOTE = 4.5

curl -X PUT /parametros-calculo/UNIDADES_POR_PACOTE \
  -d '{"chave":"UNIDADES_POR_PACOTE","valor":4.5,"descricao":"Ajustado"}'
```

**Cenário 2:** Ajustar valor inteiro
```bash
# Antes: TOTAL_UNIDADE_POR_KG = 266
# Depois: TOTAL_UNIDADE_POR_KG = 280

curl -X PUT /parametros-calculo/TOTAL_UNIDADE_POR_KG \
  -d '{"chave":"TOTAL_UNIDADE_POR_KG","valor":280,"descricao":"Nova máquina"}'
```

---

**Conclusão:** O OpenAPI spec agora deixa **explícito** que o campo `valor` aceita decimais através de:
1. ✅ `type: number` com `format: double`
2. ✅ Exemplo com valor decimal (4.4)
3. ✅ Descrição explicativa
4. ✅ Múltiplos exemplos (POST e PUT)

