import java.util.*;
import java.io.*;
import java.net.*;

class ServidorProxy {

	int emUso=0;
	int usaMultiThread=1;

	class Executor implements Runnable {
		Socket comuniCliente, dominConec;
		InputStream inDomin, inCliente;
		OutputStream outDomin, outCliente;
		InputStreamReader leitor;
		BufferedReader bLeitor, dLeitor;
		PrintWriter dEscritor, bEscritor;
		int porta, flagVindo=0, flagSeiHost=0;
		String domainName, formatoReq, extensao, ipAddr, requisicaoDoCliente="", dadosT="", conteudoResposta="";

		class LeitorServ implements Runnable { //Essa classe lê tudo que é enviado pelo cliente e encaminha ao servidor

			public void obtemRepassaResposta() {
				System.out.println("Vou obter a resposta\n");
				String msg="", myAll="";
				String[] campos;
				int temConteudo=0, tamanhoConteudo=0, acumulador=0, conteudoB=0, quantos;
				try{
					if(extensao.toLowerCase()=="html"||extensao.toLowerCase()=="txt") {
						conteudoB=0;
						System.out.println("O conteúdo da resposta é textual\n\n");

						while((msg=dLeitor.readLine()).length()!=0) {
							myAll+=msg;
							myAll+="\r\n";
							campos=msg.split(":");
							if(campos.length>1&&campos[0].indexOf("Content-Length")!=-1)
							{
								temConteudo=1;
								tamanhoConteudo=Integer.parseInt(campos[1].trim());
							}
							if(campos.length>1&&campos[0].indexOf("Content-Type")!=-1) {
								temConteudo=1;
							}
						}
						if(temConteudo==1&&tamanhoConteudo==0) {
							tamanhoConteudo=20000000;
						}
						myAll+="\r\n";
						System.out.println("O cabeçalho da resposta !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!: \n\n"+myAll+"\n\n");
						if(temConteudo==1) {
							System.out.println("Conteúdo: \n\n");

							while(acumulador<tamanhoConteudo&&(msg=dLeitor.readLine())!=null) {
								conteudoResposta+=msg+"\n";
								System.out.println(msg+"\n");
								acumulador=conteudoResposta.length();
								if(msg.indexOf("</html>")!=-1) {
									break;
								}
							}
							System.out.println("\nfim do conteúdo\n");
						} else {
							System.out.println("Parece que a resposta não possui conteudo\n");
						}
					} else {
						System.out.println("O conteúdo da resposta não é textual, vou apenas repassar, e tem tamanho: "+tamanhoConteudo+"\n\n");
						conteudoB=1;
						byte[] temp=new byte[4096];
						while((quantos=inDomin.read(temp))!=-1) {
							outCliente.write(temp, 0, quantos);
							outCliente.flush();
						}
						outCliente.close();
						System.out.println("\nTerminei de repassar o conteúdo não textual\n");
					}
				} catch(Exception exc) {
					System.out.println("Erro ao receber a mensagem do servidor\n\n");
					//exc.printStackTrace();
				}

				//envio a resposta de volta para o cliente
				try{
					if(conteudoB!=1) {
						bEscritor.print(myAll);
						bEscritor.flush();
						if(temConteudo==1) {
							bEscritor.print(conteudoResposta);
							bEscritor.flush();
						}
						bEscritor.close();
					}
				} catch(Exception exc) {
					System.out.println("Erro ao enviar a mensagem para o cliente\n\n");
				}
			}

			public void run() {
				String linha="";
				String[] campos;
				int temConteudo=0, tamanhoConteudo=0, acumulador=0, validez;
				try {
					while(flagSeiHost==0) {
						linha=bLeitor.readLine();
						if(linha!=null) {
							obtemHostInfo(linha);
							requisicaoDoCliente+=linha+"\r\n";
							flagSeiHost=1;
						} else {
							throw new Exception();
						}
					}

					if(1==1) { //substituir pela condição relacionada aos arquivos
						//estabelece conexão com o domínio requisitado pelo cliente
						dominConec=new Socket(ipAddr, porta);
						outDomin=dominConec.getOutputStream();
						dEscritor=new PrintWriter(outDomin);
						inDomin=dominConec.getInputStream();
						dLeitor=new BufferedReader(new InputStreamReader(inDomin));

						dEscritor.print(linha+"\r\n");

						while((linha=bLeitor.readLine()).length()!=0) {
							validez=1;
							campos=linha.split(":");
							if(campos.length>1&&campos[0].indexOf("Content-Length")!=-1)
							{
								temConteudo=1;
								tamanhoConteudo=Integer.parseInt(campos[1].trim());
							}
							if(campos.length>1&&campos[0].indexOf("Accept-Encoding")!=-1) {
								validez=0;
							}
							if(validez==1) {
								requisicaoDoCliente+=linha+"\r\n";
								dEscritor.print(linha+"\r\n");
								dEscritor.flush();
							}
						}
						dEscritor.print("Accept-Encoding: \r\n");
						dEscritor.flush();
						requisicaoDoCliente+="Accept-Encoding: \r\n";
						dEscritor.print("\r\n");
						dEscritor.flush();

						System.out.println("Campos de cabeçalho da requisição que foram enviados: \n\n"+requisicaoDoCliente+"\r\n\n\n");

						if(temConteudo==1) {
							if(extensao.toLowerCase()=="html"||extensao.toLowerCase()=="txt") {
								System.out.println("O conteúdo é textual (da requisição) \n\n");
								while(acumulador<tamanhoConteudo&&(linha=bLeitor.readLine())!=null) {
									dadosT+=linha+"\n";
									System.out.println(linha+"\n");
									acumulador=dadosT.length();
								}
								dEscritor.print(dadosT);
								dEscritor.flush();								
								flagVindo=1;
								System.out.println("\nfim do conteúdo\n");
							} else {

							}
						} else {
							flagVindo=1;
							System.out.println("Parece que a requisição não tem conteúdo\n\n");
						}
						obtemRepassaResposta();
					}  else {

					}

				} catch(Exception exc) {
					System.out.println("Ei, houve um problema ao tentar repassar a mensagem para o servidor e/ou identificá-lo\n\n");
				} finally {
					try {
						comuniCliente.close();
						dominConec.close();
					} catch(Exception exc) {
						System.out.println("Falha ao fechar os soquetes\n");
					}
					//System.out.println("Finalizei esse Thread\n\n");
					emUso=0;
				}
			}
		}

		public Executor(Socket clienteSock) {
			this.comuniCliente=clienteSock;
		}

		public void obtemHostInfo(String arg) {
			String[] pedacos=arg.split(" ");
			System.out.println((pedacos[1].indexOf("http://")==-1)?pedacos[1]:pedacos[1].split("//")[1]);
			domainName=(pedacos[1].indexOf("http://")==-1)?pedacos[1]:pedacos[1].split("//")[1];
			String[] domainNames=domainName.split("/");
			System.out.println(domainNames[0]);
			domainName=domainNames[0];
			if(domainNames.length>1) {
				formatoReq=domainNames[domainNames.length-1];
				System.out.println("FormatoReq: "+formatoReq);
				if(formatoReq.indexOf(".")!=-1) {
					extensao=formatoReq.substring(formatoReq.indexOf("."), formatoReq.length());
				} else {
					extensao="html";
				}
				if(extensao==null||extensao.length()==0) {
					extensao="html";
				}
			} if(formatoReq==null||formatoReq.length()==0) {
				formatoReq=".html";
				extensao="html";
			}
			String[] portsDomain=domainNames[0].split(":");
			System.out.println(portsDomain[0]);
			domainName=portsDomain[0];
			System.out.println("Extensão: "+ extensao);
			if(portsDomain.length>1) {
				porta=Integer.parseInt(portsDomain[1]);
			} else {
				porta=80;
			}
			System.out.println(porta);
			try{
				InetAddress resolvedor=InetAddress.getByName(domainName);
				ipAddr=resolvedor.getHostAddress();
				System.out.println(ipAddr);
				System.out.println("\n");
			} catch(UnknownHostException exc) {
				System.out.println("Host desconhecido\n\n");
			}
		}

		public void run() {
			emUso=1;
			try {
				//inicializa o leitor e o escritor para o cliente
				inCliente=comuniCliente.getInputStream();
				leitor=new InputStreamReader(inCliente);
				bLeitor=new BufferedReader(leitor);
				outCliente=comuniCliente.getOutputStream();
				bEscritor=new PrintWriter(outCliente);


				Thread tServ=new Thread(new LeitorServ());
				tServ.start();

			} catch(Exception exc) {
				System.out.println("Alguma coisa deu errado, tem certeza que esse domínio existe?\n\n");
			} finally {
				//System.out.println("Terminei o externo também\n");
			} 
		}
	}

	public void executa() {
		try {
			ServerSocket sockeServ = new ServerSocket(2048);
			while(true) {
				if(usaMultiThread==1) {
					Thread t = new Thread(new Executor(sockeServ.accept()));
					t.start();
				} else if(emUso==0) {
					Thread t = new Thread(new Executor(sockeServ.accept()));
					t.start();
				}
			}
		} catch(IOException exc) {
			exc.printStackTrace();
		}
	}

	public static void main(String args[]) {
		new ServidorProxy().executa();
	}
}