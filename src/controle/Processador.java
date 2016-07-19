package controle;

public class Processador {



	/**
	 * M�todo que representa a solicita��o de acesso do tipo MOV REG 'BYTE',
	 * calculada assim:
	 * 
	 * POSI��O DO ARRAY DE MEM�RIA(RANDOM) * TAMANHO(8K) * N�MERO DE BYTES(1024)
	 *
	 * POSI��O 0 (0K-8K) = 0
	 * 
	 * POSI��O 1 (8K-16K) = 8192
	 * 
	 * POSI��O 2 (16K-24K) = 16384
	 * 
	 * @return
	 *
	 */
	public static boolean instrucaoDeAcesso(int posicaoSolicitada) {
		
		//A MMU retorna a posi��o fisica que est� a p�gina solicitada
		boolean hit = MMU.mapeamento(posicaoSolicitada, MMU.GRAVAR_TEMPO);
		
		return hit;
	}
}
