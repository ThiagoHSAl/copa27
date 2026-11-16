package br.ufmg.dcc603.copa27.servico;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Hash de senha com sal (RNF07). Usa PBKDF2 com HMAC-SHA256, função de
 * derivação de chave da biblioteca padrão do Java equivalente ao bcrypt: lenta
 * de propósito e com sal aleatório por senha. Formato gravado:
 * {@code pbkdf2$iteracoes$sal$hash}, com sal e hash em Base64.
 */
public final class HashSenha {

    private static final int ITERACOES = 120_000;
    private static final int TAMANHO_SAL = 16;
    private static final int TAMANHO_HASH_BITS = 256;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private HashSenha() {
    }

    public static String gerar(String senha) {
        if (senha == null || senha.isEmpty()) {
            throw new IllegalArgumentException("A senha não pode ser vazia.");
        }
        byte[] sal = new byte[TAMANHO_SAL];
        ALEATORIO.nextBytes(sal);
        byte[] hash = derivar(senha, sal, ITERACOES);
        Base64.Encoder b64 = Base64.getEncoder();
        return "pbkdf2$" + ITERACOES + "$" + b64.encodeToString(sal) + "$" + b64.encodeToString(hash);
    }

    public static boolean verificar(String senha, String armazenado) {
        if (senha == null || armazenado == null) {
            return false;
        }
        String[] partes = armazenado.split("\\$");
        if (partes.length != 4 || !partes[0].equals("pbkdf2")) {
            return false;
        }
        try {
            int iteracoes = Integer.parseInt(partes[1]);
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            return MessageDigest.isEqual(esperado, derivar(senha, sal, iteracoes));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derivar(String senha, byte[] sal, int iteracoes) {
        PBEKeySpec spec = new PBEKeySpec(senha.toCharArray(), sal, iteracoes, TAMANHO_HASH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 indisponível nesta JVM.", e);
        } finally {
            spec.clearPassword();
        }
    }
}
