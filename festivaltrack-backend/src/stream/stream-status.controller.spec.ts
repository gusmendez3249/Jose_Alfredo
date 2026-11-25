import { StreamStatusController } from './stream-status.controller';

describe('StreamStatusController', () => {
  let controller: StreamStatusController;

  beforeEach(() => {
    jest.useFakeTimers();
    jest.setSystemTime(new Date('2026-11-25T20:00:00Z'));
    controller = new StreamStatusController();
  });

  afterEach(() => jest.useRealTimers());

  it('sin transmisión reporta isLive=false', () => {
    expect(controller.getStatus()).toMatchObject({
      isLive: false,
      streamUrl: '',
      eventoId: null,
    });
  });

  it('guarda la URL y el concierto que se transmite', () => {
    controller.setStatus({
      streamUrl: 'rtsp://192.168.1.20:1935',
      isLive: true,
      eventoId: 'evt1',
      eventoNombre: 'Gran Gala Mariachi',
    });
    expect(controller.getStatus()).toMatchObject({
      isLive: true,
      streamUrl: 'rtsp://192.168.1.20:1935',
      emulatorUrl: 'rtsp://10.0.2.2:1935',
      eventoId: 'evt1',
      eventoNombre: 'Gran Gala Mariachi',
    });
  });

  it('se apaga sola si el admin deja de reportar', () => {
    controller.setStatus({ streamUrl: 'rtsp://10.0.2.2:1935', isLive: true });
    jest.advanceTimersByTime(StreamStatusController.SIN_SENAL_MS + 1);
    expect(controller.getStatus()).toMatchObject({
      isLive: false,
      streamUrl: '',
    });
  });

  it('al detener limpia la URL y el concierto', () => {
    controller.setStatus({
      streamUrl: 'rtsp://10.0.2.2:1935',
      isLive: true,
      eventoId: 'evt1',
    });
    controller.setStatus({ streamUrl: '', isLive: false });
    expect(controller.getStatus()).toMatchObject({
      isLive: false,
      eventoId: null,
    });
  });
});
