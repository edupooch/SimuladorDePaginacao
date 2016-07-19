package controle;

public class Processador {



	/**
	 * Método que representa a solicitação de acesso do tipo MOV REG 'BYTE',
	 * calculada assim:
	 * 
	 * POSIÇÃO DO ARRAY DE MEMÓRIA(RANDOM) * TAMANHO(8K) * NÚMERO DE BYTES(1024)
	 *
	 * POSIÇÃO 0 (0K-8K) = 0
	 * 
	 * POSIÇÃO 1 (8K-16K) = 8192
	 * 
	 * POSIÇÃO 2 (16K-24K) = 16384
	 * 
	 * @return
	 *
	 */
	public static boolean instrucaoDeAcesso(int posicaoSolicitada) {
		
		//A MMU retorna a posição fisica que está a página solicitada
		boolean hit = MMU.mapeamento(posicaoSolicitada, MMU.GRAVAR_TEMPO);
		
		return hit;
	}
}
