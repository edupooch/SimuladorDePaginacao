package controle;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Random;

import modelo.Pagina;
import visao.Janela;

public class SistemaOperacional implements Runnable {

	private static int misses;
	private static int hits;
	private static int tamanhoVirtual;

	public SistemaOperacional(int tamanhoDaMemoriaVirtual, int tamanhoDaMemoriaFisica, int tamanhoPagina) {
		System.out.println("----------------------------------------------");
		tamanhoVirtual = tamanhoDaMemoriaVirtual;
		new MMU(tamanhoDaMemoriaVirtual, tamanhoDaMemoriaFisica, tamanhoPagina);
		criaArquivos(tamanhoDaMemoriaVirtual);

	}

	public static void inicializa() {

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// O objeto Random sorteia um inteiro de 0 a 127 que representa a
		// posição da memória virtual que será solicitado o acesso
		Random random = new Random();
		int posicaoSolicitada = random.nextInt(tamanhoVirtual);
		boolean hit = Processador.instrucaoDeAcesso(posicaoSolicitada);

		System.out.println("----------------------------------------------");

		if (hit) {
			Janela.hit.setPieValue(++hits);

		} else {
			Janela.miss.setPieValue(++misses);
		}
		double porcentagemDeMiss = 100
				* (Janela.miss.getPieValue() / (Janela.miss.getPieValue() + Janela.hit.getPieValue()));
		System.out.println(String.format("Taxa de miss: %.2f", porcentagemDeMiss));

	}

	/**
	 * Método inicial para gerar os arquivos que representarão as páginas salvas
	 * em disco
	 */
	private static void criaArquivos(int tamanhoDaMemoriaVirtual) {

		for (int i = 0; i < tamanhoDaMemoriaVirtual; i++) {
			try {
				long comeco = System.currentTimeMillis();

				// Inicia o gerador de arquivos que salvará os arquivos .dat no
				// disco que terão o conteúdo da página
				FileOutputStream arquivo = new FileOutputStream("src/disco/" + i + ".dat");
				ObjectOutputStream geradorDeArquivo = new ObjectOutputStream(arquivo);

				// Inicia as páginas e define os conteúdos delas com o tempo que
				// demorou a instrução para criá-las
				Pagina pagina = new Pagina();
				// Grava o caminho do arquivo de pagina na memória virtual
				pagina.setArquivoConteudo("src/disco/" + i + ".dat");

				MMU.memoriaVirtual[i] = pagina;

				// Salva o tempo(em ms) que demorou para a criação da página em
				// um arquivo .dat no pacote disco
				long tempoDeInstrucao = System.currentTimeMillis() - comeco;
				geradorDeArquivo.writeObject(tempoDeInstrucao);
				geradorDeArquivo.flush();
				geradorDeArquivo.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public synchronized void run() {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		while (true) {
			inicializa();
		}
	}

}
