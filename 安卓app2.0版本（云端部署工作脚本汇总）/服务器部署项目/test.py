from PIL import Image
from util import util
from options.test_options import TestOptions
from data import create_dataset
from models import create_model


class CycleGAN:
    def __init__(self):
        self.opt = TestOptions().parse()  # get test options
        self.dataset = create_dataset(self.opt)  # create a dataset given opt.dataset_mode and other options
        self.model = create_model(self.opt)  # create a model given opt.model and other options
        self.model.setup(self.opt)  # regular setup: load and print networks; create schedulers

    def detect(self):
        if self.opt.eval:
            self.model.eval()
        for i, data in enumerate(self.dataset):
            if i >= self.opt.num_test:  # only apply our model to opt.num_test images.
                break
            self.model.set_input(data)  # unpack data from data loader
            self.model.test()  # run inference
            visuals = self.model.get_current_visuals()  # get image results

            for label, im_data in visuals.items():
                im = util.tensor2im(im_data)
                if label == 'fake':
                    image_name = '%s_%s.png' % ('result', label)
                    image_pil = Image.fromarray(im)
                    image_pil.save(image_name)


if __name__ == '__main__':
    CA = CycleGAN()
    # CA.detect()



