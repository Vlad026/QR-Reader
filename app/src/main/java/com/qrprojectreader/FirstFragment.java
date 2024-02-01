package com.qrprojectreader;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import com.qrprojectreader.databinding.FragmentFirstBinding;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;

    String workerNumber;
    String requestCode = "0";
    String projectCode;
    String SERVER_IP;
    int SERVER_PORT;
    int timeout;
    String result = null;
    SharedPreferences prefer;
    String rawValue;
    private PrintWriter output;
    private BufferedReader input;
    InputStream inputStream;
    Socket socket = null;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefer = PreferenceManager.getDefaultSharedPreferences(getContext());
        String currentProject = prefer.getString("current_project", "");
        if (!currentProject.isEmpty()) {
            binding.textViewProjectName.setText(currentProject);
            binding.textViewProjectName.setTextColor(Color.GRAY);
            binding.textViewStatus.setText("Зарегистрирован ранее");
            binding.textViewStatus.setTextColor(Color.GRAY);
        }

        binding.buttonQrscan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String errorsPref = "";
                SERVER_IP = prefer.getString("address", "0");
                if (SERVER_IP.equals("0")) errorsPref += "Не указан IP-адрес\n";
                SERVER_PORT = Integer.parseInt(prefer.getString("port", "0"));
                if (SERVER_PORT == 0) errorsPref += "Не указан порт\n";
                workerNumber = prefer.getString("worker_number", "0");
                if (workerNumber.equals("0")) errorsPref += "Не указан номер сотрудника\n";
                timeout = 1000 * Integer.parseInt(prefer.getString("timeout", "0"));
                if (timeout < 1000 || timeout > 120000)
                    errorsPref += "Таймаут ожидания ответа от сервера вне допустимого диапазона 1...120 сек.\n";
                if (!errorsPref.isEmpty()) {
                    binding.textViewStatus.setText(errorsPref);
                    binding.textViewStatus.setTextColor(Color.RED);
                } else {
                    GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                            .setBarcodeFormats(
                                    Barcode.FORMAT_QR_CODE)
                            .build();
                    GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(getContext(), options);
                    scanner
                            .startScan()
                            .addOnSuccessListener(
                                    barcode -> {
                                        rawValue = barcode.getRawValue();
                                        if (!checkCorrectQR(rawValue)) {
                                            binding.textViewProjectName.setText("");
                                            binding.textViewStatus.setText("Некорректный QR-код");
                                            binding.textViewStatus.setTextColor(Color.RED);
                                        } else {
                                            binding.textViewProjectName.setText(rawValue.substring(rawValue.indexOf(". ") + 2));
                                            binding.textViewProjectName.setTextColor(Color.GRAY);
                                            binding.textViewStatus.setText("");
                                            projectCode = rawValue.substring(3, rawValue.indexOf('.'));
                                            String message = requestCode + " " + projectCode + " " + workerNumber + "\n";
                                            new Thread(new ThreadSendReceive(message)).start();
                                        }
                                    })
                            .addOnCanceledListener(
                                    () -> {
                                        // Task canceled
                                        // binding.textViewStatus.setText("Сканирование отменено");
                                    })
                            .addOnFailureListener(
                                    e -> {
                                        // Task failed with an exception
                                        binding.textViewStatus.setText(e.getMessage());
                                    });
                }
            }
        });
    }

    class ThreadSendReceive implements Runnable {
        private String message;
        private int time = 0;
        int bytes = 0;
        private int color = Color.RED;

        ThreadSendReceive(String Message) {
            this.message = Message;
        }

        @Override
        public void run() {
            result = null;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    binding.buttonQrscan.setEnabled(false);
                }
            });
            // TODO Auto-generated method stub
            DataOutputStream dataOutputStream = null;
            DataInputStream dataInputStream = null;

            try {
                socket = new Socket();
                InetSocketAddress sockAdr = new InetSocketAddress(SERVER_IP, SERVER_PORT);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.textViewStatus.setText("Подключение к серверу");
                        binding.textViewStatus.setTextColor(Color.GRAY);
                    }
                });
                socket.connect(sockAdr, timeout);
                output = new PrintWriter(socket.getOutputStream());
                inputStream = socket.getInputStream();
                output.write(message);
                output.flush();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.textViewStatus.setText("Получение ответа от сервера");
                    }
                });
                do {
                    bytes = inputStream.available();
                    if (bytes > 0) {
                        byte[] buffer = new byte[bytes];
                        inputStream.read(buffer, 0, bytes);
                        //resultCode = new String(buffer, 0, bytes);
                        if (buffer[0] == '0' && buffer[1] == ' ' && bytes > 2) {
                            switch (buffer[2]) {
                                case '0': {
                                    result = "Регистрация прошла успешно";
                                    color = Color.GRAY;
                                    prefer.edit().putString("current_project", rawValue.substring(rawValue.indexOf(". ") + 2)).apply();
                                    break;
                                }
                                case '1':
                                    result = "Некорректный код проекта";
                                    break;
                                case '2':
                                    result = "Некорректный табельный номер";
                                    break;
                                case '3':
                                    result = "Табельный номер не связан с проектом";
                                    break;
                                case '4':
                                    result = "Не удалось подключиться к серверу базы данных ";
                                    break;
                                case '5':
                                    result = "Ошибка авторизации при подключении к серверу базы данных";
                                    break;
                                default:
                                    result = "Неизвестная ошибка от сервера";
                                    break;
                            }
                        } else {
                            result = "Некорректный формат ответа от сервера";
                        }
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                binding.textViewStatus.setText(result);
                                binding.textViewStatus.setTextColor(color);
                            }
                        });
                    }
                    Thread.sleep(100);
                    time += 100;
                } while (time <= timeout && bytes == 0);
                if (result == null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            binding.textViewStatus.setText("Сервер не отвечает");
                            binding.textViewStatus.setTextColor(color);
                        }
                    });
                }

            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.textViewStatus.setText("Некорректный IP-адрес");
                        binding.textViewStatus.setTextColor(color);
                    }
                });
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.textViewStatus.setText("Нет соединения с сервером");
                        binding.textViewStatus.setTextColor(color);
                    }
                });
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                if (output != null) {
                    output.close();
                }

                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    binding.buttonQrscan.setEnabled(true);
                }
            });
        }
    }

    public boolean checkCorrectQR(String QR) {
        // Format: ЛПА[Number]. Name
        int i;
        try {
            if (QR.charAt(0) != 'Л') return false;
            if (QR.charAt(1) != 'П') return false;
            if (QR.charAt(2) != 'А') return false;
            if (QR.charAt(3) == '-') {
                for (i = 4; Character.isDigit(QR.charAt(i)); i++) ;
                if (i == 4) return false;
            } else if (Character.isDigit(QR.charAt(3))) {
                for (i = 3; Character.isDigit(QR.charAt(i)); i++) ;
                if (i == 3) return false;
            } else return false;
            if (QR.charAt(i) != '.') return false;
            if (QR.charAt(++i) != ' ') return false;
            if (QR.length() == ++i) return false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void closeConnection() {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.getMessage();
            } finally {
                socket = null;
            }
        }
        socket = null;
    }
}